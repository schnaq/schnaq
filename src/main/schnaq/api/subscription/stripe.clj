(ns schnaq.api.subscription.stripe
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [ring.util.http-response :refer [ok not-found bad-request]]
            [schnaq.api.subscription.stripe-lib :as stripe-lib]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [taoensso.timbre :as log])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException]
           [com.stripe.model.checkout Session]))

(def ^:private error-article-not-found
  (at/build-error-body :article/not-found "Article could not be found."))

(set! (. Stripe -apiKey) config/stripe-secret-api-key)

;; -----------------------------------------------------------------------------

(>defn- build-checkout-session-parameters
  "Configure all checkout-session parameters. Adds items, defines URLs and adds
        costumer metadata to the user."
  [price-id keycloak-id email]
  [:stripe.price/id :user.registered/keycloak-id :user.registered/email => map?]
  (let [items [{"price" price-id
                "quantity" 1}]]
    {"success_url" (format "%s/subscription/success" config/frontend-url)
     "cancel_url" (format "%s/subscription/cancel" config/frontend-url)
     "mode" "subscription"
     "client_reference_id" keycloak-id
     "customer_email" email
     "metadata" {"keycloak-id" keycloak-id}
     "subscription_data" {"metadata" {"keycloak-id" keycloak-id}}
     "line_items" items}))

(defn- create-checkout-session
  "Open stripe's checkout page with the currently selected item."
  [{:keys [identity parameters]}]
  (try
    (let [price-id (get-in parameters [:body :price-id])
          checkout-session-parameters (build-checkout-session-parameters price-id (:id identity) (:email identity))
          session (Session/create checkout-session-parameters)]
      (ok {:redirect (.getUrl session)}))
    (catch InvalidRequestException _
      (bad-request error-article-not-found))))

(defn- get-product-price
  "Query a product's price by its stripe-identifier, which is a string, e.g.
  `\"price_4242424242\"`."
  ([request]
   (get-product-price request stripe-lib/retrieve-price))
  ([{{{:keys [price-id]} :query} :parameters} price-by-id]
   (if-let [price (price-by-id price-id)]
     (ok price)
     (not-found error-article-not-found))))

;; -----------------------------------------------------------------------------

(def events (atom {}))
(def n2o-id "TODO: Delete me"
  "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")

(defmulti ^:private stripe-event
  "Dispatch incoming stripe events."
  (fn [event] (:type event)))

(defmethod stripe-event "customer.subscription.created" [event]
  '"This event is triggered when a new user creates a subscription on stripe. We
   extract all information from the event and store the relevant information in
   our database."
  (let [keycloak-id (get-in event [:data :object :metadata :keycloak-id])
        stripe-customer-id (get-in event [:data :object :customer])
        stripe-subscription-id (get-in event [:data :object :id])]
    (user-db/subscribe-pro-tier keycloak-id stripe-subscription-id stripe-customer-id))
  (log/info "Subscription successfully created 🤑"))

(defmethod stripe-event :default [event]
  (swap! events assoc (keyword (:type event)) event))

;; -----------------------------------------------------------------------------

(defn- webhook
  "Handle incoming stripe requests. This function receives all events from 
  stripe and dispatches them further."
  [{:keys [body-params]}]
  (log/info "Event type:" (:type body-params))
  (stripe-event body-params)
  (ok {:message "Always return 200 to stripe."}))

(defn- cancel-user-subscription
  "Cancel a user's subscription."
  [{:keys [body-params identity]}]
  (if-let [cancelled-subscription (stripe-lib/cancel-subscription! (:sub identity) (:cancel? body-params))]
    (ok (stripe-lib/subscription->edn cancelled-subscription))
    (bad-request (at/build-error-body :stripe.subscription/user-not-existing "The requested user does not have a subscription."))))

(defn- retrieve-subscription-status
  "Return the subscription-status."
  [{{:keys [sub]} :identity}]
  (if-let [subscription (stripe-lib/keycloak-id->subscription sub)]
    (ok (stripe-lib/subscription->edn subscription))
    (ok)))

;; -----------------------------------------------------------------------------

(def stripe-routes
  [["/stripe" {:swagger {:tags ["subscription" "stripe"]}}
    [""
     ["/create-checkout-session"
      {:post create-checkout-session
       :name :api.stripe/create-checkout-session
       :description (at/get-doc #'create-checkout-session)
       :parameters {:body {:price-id :stripe.price/id}}
       :responses {200 {:body {:redirect string?}}
                   400 at/response-error-body}
       :middleware [:user/authenticated?]}]
     ["/price"
      {:get get-product-price
       :name :api.stripe/get-product-price
       :description (at/get-doc #'get-product-price)
       :parameters {:query {:price-id :stripe.price/id}}
       :responses {200 {:body :stripe/price}
                   403 at/response-error-body
                   404 at/response-error-body}}]
     ["/subscription" {:middleware [:user/authenticated?]}
      ["/status"
       {:get retrieve-subscription-status
        :name :api.stripe/retrieve-subscription-status
        :description (at/get-doc #'retrieve-subscription-status)
        :responses {200 {:body (s/or :subscription :stripe/subscription
                                     :no-subscription nil?)}}}]
      ["/cancel"
       {:post cancel-user-subscription
        :name :api.stripe/cancel-user-subscription
        :description (at/get-doc #'cancel-user-subscription)
        :parameters {:body {:cancel? boolean?}}
        :responses {200 {:body :stripe/subscription}}}]]]
    ["/webhook"
     {:post webhook
      :middleware [stripe-lib/verify-signature-middleware]
      :name :api.stripe/webhook
      :description (at/get-doc #'webhook)
      :responses {200 {:body {:message string?}}}}]]])

