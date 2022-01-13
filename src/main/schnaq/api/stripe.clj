(ns schnaq.api.stripe
  (:require [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [ok forbidden not-found bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [taoensso.timbre :as log]
            [schnaq.database.user :as user-db])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException]
           [com.stripe.model Price Customer]
           [com.stripe.model.checkout Session]
           [com.stripe.net Webhook]))

(def ^:private error-article-not-found
  (at/build-error-body :article/not-found "Article could not be found."))

(set! (. Stripe -apiKey) config/stripe-secret-api-key)

(>defn- build-checkout-session-parameters
  "Configure all checkout-session parameters. Adds items, defines URLs and adds
        costumer metadata to the user."
  [product-price-id keycloak-id email]
  [:stripe/product-price-id :user.registered/keycloak-id :user.registered/email :ret map?]
  (let [items [{"price" product-price-id
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
    (let [product-price-id (get-in parameters [:body :product-price-id])
          checkout-session-parameters (build-checkout-session-parameters product-price-id (:id identity) (:email identity))
          session (Session/create checkout-session-parameters)]
      (ok {:redirect (.getUrl session)}))
    (catch InvalidRequestException _
      (bad-request error-article-not-found))))

(defn- get-product-price
  "Query a product's price by its stripe-identifier, which is a string, e.g.
  `\"price_4242424242\"`."
  [{{{:keys [product-price-id]} :query} :parameters}]
  (try
    (let [article (Price/retrieve product-price-id)]
      (if (.getActive article)
        (ok {:price (-> article .getUnitAmount (/ 100) float)
             :product-price-id product-price-id})
        (forbidden (at/build-error-body :article/not-active "Article is not active."))))
    (catch InvalidRequestException _
      (not-found error-article-not-found))))

;; -----------------------------------------------------------------------------

;; TODO: delete me
(def events (atom {}))
(def n2o-id "TODO: Delete me"
  "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")

(defmulti stripe-event
  "Dispatch incoming stripe events."
  (fn [event] (:type event)))

(defmethod stripe-event "customer.subscription.created" [event]
  '"This event is triggered when a new user creates a subscription on stripe. We
   extract all information from the event and store the relevant information in
   our database."
  (def subscription-event event)
  (let [keycloak-id (get-in event [:data :object :metadata :keycloak-id])
        stripe-customer-id (get-in event [:data :object :customer])
        stripe-subscription-id (get-in event [:data :object :id])]
    (user-db/subscribe-pro-tier keycloak-id stripe-subscription-id stripe-customer-id))
  (log/info "Subscription successfully created 🤑"))

(defmethod stripe-event :default [event]
  (swap! events assoc (keyword (:type event)) event))

(defn- webhook
  "Handle incoming stripe requests. This function receives all events from 
  stripe and dispatches them further."
  [{:keys [body-params]}]
  (log/info "Event type:" (:type body-params))
  (stripe-event body-params)
  (ok {:message "Always return 200 to stripe."}))

(comment
  (reset! events {})
  @events
  (keys @events)

  (:customer.subscription.updated @events)

  (let [keycloak-id (get-in subscription-event [:data :object :metadata :keycloak-id])
        stripe-customer-id (get-in subscription-event [:data :object :customer])
        stripe-subscription-id (get-in subscription-event [:data :object :id])]
    (user-db/subscribe-pro-tier keycloak-id stripe-subscription-id stripe-customer-id))
  (user-db/private-user-by-keycloak-id n2o-id)
  (user-db/unsubscribe-pro-tier n2o-id)

  nil)

;; -----------------------------------------------------------------------------


(def stripe-routes
  [["/stripe" {:swagger {:tags ["subscription" "stripe"]}}
    [""
     ["/create-checkout-session"
      {:post create-checkout-session
       :description (at/get-doc #'create-checkout-session)
       :parameters {:body {:product-price-id :stripe/product-price-id}}
       :responses {200 {:body {:redirect string?}}
                   400 at/response-error-body}
       :middleware [:user/authenticated?]}]
     ["/price"
      {:get get-product-price
       :description (at/get-doc #'get-product-price)
       :parameters {:query {:product-price-id :stripe/product-price-id}}
       :responses {200 {:body {:price number?
                               :product-price-id :stripe/product-price-id}}
                   403 at/response-error-body
                   404 at/response-error-body}}]]
    ["/webhook"
     {:post webhook
      :name :stripe/webhook
      :description (at/get-doc #'webhook)
      :responses {200 {:body {:message string?}}}}]]])

