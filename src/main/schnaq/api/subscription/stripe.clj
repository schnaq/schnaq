(ns schnaq.api.subscription.stripe
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [ring.util.http-response :refer [bad-request ok]]
            [schnaq.api.subscription.stripe-lib :as stripe-lib]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.config.stripe :refer [prices]]
            [schnaq.database.user :as user-db]
            [schnaq.mail.cleverreach :as cleverreach]
            [schnaq.toolbelt :as toolbelt]
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
    {"success_url" (format "%s/welcome/pro?subbed=true" config/frontend-url)
     "cancel_url" (format "%s/subscription/cancel" config/frontend-url)
     "mode" "subscription"
     "client_reference_id" keycloak-id
     "customer_email" email
     "allow_promotion_codes" true
     "automatic_tax" {"enabled" true} ;; Activate automatic tax collection
     "tax_id_collection" {"enabled" true} ;; Collect tax id from registered companies
     "metadata" {"keycloak-id" keycloak-id}
     "subscription_data" {"metadata" {"keycloak-id" keycloak-id}}
     "line_items" items}))

(defn- create-checkout-session
  "Open stripe's checkout page with the currently selected item."
  [{:keys [identity parameters]}]
  (try
    (let [price-id (get-in parameters [:query :price-id])
          checkout-session-parameters (build-checkout-session-parameters price-id (:id identity) (:email identity))
          session (Session/create checkout-session-parameters)]
      (ok {:redirect (.getUrl session)}))
    (catch InvalidRequestException _
      (bad-request error-article-not-found))))

(defn- get-product-prices
  "Query all product prices stored in the stripe config."
  [_request]
  (let [price-id-with-costs (walk/postwalk
                             #(if (s/valid? :stripe.price/id %)
                                (stripe-lib/retrieve-price %)
                                %)
                             prices)]
    (ok {:prices price-id-with-costs})))

(defn- post-to-mattermost-if-in-production
  "Post a message to mattermost if stripe is in production mode."
  []
  (when (.startsWith config/stripe-secret-api-key "sk_live")
    (toolbelt/post-in-mattermost! "Someone just started a subscription :tada:")))

;; -----------------------------------------------------------------------------

(defmulti ^:private stripe-event
  "Dispatch incoming stripe events."
  :type)

(defmethod stripe-event "customer.subscription.created" [event]
  '"This event is triggered when a new user creates a subscription on stripe. We
   extract all information from the event and store the relevant information in
   our database."
  (let [keycloak-id (get-in event [:data :object :metadata :keycloak-id])
        stripe-customer-id (get-in event [:data :object :customer])
        stripe-subscription-id (get-in event [:data :object :id])]
    (user-db/subscribe-pro-tier keycloak-id stripe-subscription-id stripe-customer-id)
    (cleverreach/add-pro-tag! (:user.registered/email (user-db/private-user-by-keycloak-id keycloak-id)))
    (cleverreach/remove-free-tag! (:user.registered/email (user-db/private-user-by-keycloak-id keycloak-id)))
    (post-to-mattermost-if-in-production)
    (log/info "Subscription successfully created 🤑 User:" keycloak-id)))

(defmethod stripe-event "customer.subscription.deleted" [event]
  (let [keycloak-id (get-in event [:data :object :metadata :keycloak-id])]
    (user-db/unsubscribe-pro-tier keycloak-id)
    (cleverreach/remove-pro-tag! (:user.registered/email (user-db/private-user-by-keycloak-id keycloak-id)))
    (cleverreach/add-free-tag! (:user.registered/email (user-db/private-user-by-keycloak-id keycloak-id)))
    (log/info "Subscription deleted for user" keycloak-id)))

(defmethod stripe-event :default [event]
  (log/debug "Received unhandled event:" (:type event)))

;; -----------------------------------------------------------------------------

(defn- webhook
  "Handle incoming stripe requests. This function receives all events from 
  stripe and dispatches them further."
  [{:keys [body-params]}]
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
    (ok {})))

;; -----------------------------------------------------------------------------

(def stripe-routes
  [["/stripe" {:swagger {:tags ["subscription" "stripe"]}}
    [""
     ["/create-checkout-session"
      {:get create-checkout-session
       :name :api.stripe/create-checkout-session
       :description (at/get-doc #'create-checkout-session)
       :parameters {:query {:price-id :stripe.price/id}}
       :responses {200 {:body {:redirect string?}}
                   400 at/response-error-body}
       :middleware [:user/authenticated?]}]
     ["/prices"
      {:get get-product-prices
       :name :api.stripe/get-product-price
       :description (at/get-doc #'get-product-prices)
       :responses {200 {:body {:prices :stripe/prices}}
                   403 at/response-error-body}}]
     ["/subscription" {:middleware [:user/authenticated?]}
      ["/status"
       {:get retrieve-subscription-status
        :name :api.stripe/retrieve-subscription-status
        :description (at/get-doc #'retrieve-subscription-status)
        :responses {200 {:body (s/or :subscription :stripe/subscription
                                     :no-subscription empty?)}}}]
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
