(ns schnaq.api.stripe
  (:require [com.fulcrologic.guardrails.core :refer [>defn-]]
            [muuntaja.core :as m]
            [ring.util.http-response :refer [ok forbidden not-found bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [taoensso.timbre :as log])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException SignatureVerificationException]
           [com.stripe.model Price]
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

(defmulti ^:private stripe-event
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

;; -----------------------------------------------------------------------------

(defn- verify-signature
  "Verify the signature of the incoming stripe-request."
  [request]
  (try
    (Webhook/constructEvent (:body request)
                            (get-in request [:headers "stripe-signature"])
                            config/stripe-webhook-access-key)
    {:passed? true}
    (catch SignatureVerificationException e
      {:passed? false
       :error :stripe.verification/invalid-signature
       :message (.getMessage e)})
    (catch Exception e
      {:passed? false
       :error :stripe.verification/error
       :message (.getMessage e)})))

(defn- verify-signature-middleware
  "Verify the signature of incoming stripe requests."
  [handler]
  (fn [request]
    (let [verification-failed #(bad-request (at/build-error-body %1 %2))
          {:keys [passed? error message]} (verify-signature request)]
      (if passed?
        (handler request)
        (verification-failed error message)))))

;; -----------------------------------------------------------------------------

(defn- webhook
  "Handle incoming stripe requests. This function receives all events from 
  stripe and dispatches them further."
  [{:keys [body-params] :as request}]
  (def r request)
  (log/info "Event type:" (:type body-params))
  (stripe-event body-params)
  (ok {:message "Always return 200 to stripe."}))

(comment
  (reset! events {})
  @events
  (keys @events)

  (:customer.subscription.updated @events)

  (m/encode "application/json"
            (:body-params r))

  (def invalid-signature
    "t=1642087022,v1=c16b91b6f0e32fef75181d900117ad0da6b227b014d3acc397c781bd26af2d22,v0=493d71a2d13467fa08035a8ed6bd0e0254acaed929f8a45c6602bf9593e53132")

  (def valid-signature "t=1642094299,v1=4dbf7b73c15197e2e8c7d4b8be81e2b78cb41c83d9169b5062b05b195d692ba9,v0=ac62e25b8377b61abc0fd2f66490470c1cde384d699f927c0bc83cf51d291ca7")
  (def valid-body "{\n  \"id\": \"evt_3KHWwFFrKCGqvoMo09zWACso\",\n  \"object\": \"event\",\n  \"api_version\": \"2020-08-27\",\n  \"created\": 1642094295,\n  \"data\": {\n    \"object\": {\n      \"id\": \"pi_3KHWwFFrKCGqvoMo0WZz3a05\",\n      \"object\": \"payment_intent\",\n      \"amount\": 2000,\n      \"amount_capturable\": 0,\n      \"amount_received\": 0,\n      \"application\": null,\n      \"application_fee_amount\": null,\n      \"automatic_payment_methods\": null,\n      \"canceled_at\": null,\n      \"cancellation_reason\": null,\n      \"capture_method\": \"automatic\",\n      \"charges\": {\n        \"object\": \"list\",\n        \"data\": [\n\n        ],\n        \"has_more\": false,\n        \"total_count\": 0,\n        \"url\": \"/v1/charges?payment_intent=pi_3KHWwFFrKCGqvoMo0WZz3a05\"\n      },\n      \"client_secret\": \"pi_3KHWwFFrKCGqvoMo0WZz3a05_secret_LJMx5vKZdxn9LG33ymICgjlf6\",\n      \"confirmation_method\": \"automatic\",\n      \"created\": 1642094295,\n      \"currency\": \"usd\",\n      \"customer\": \"cus_KxRppMPnSP3VPL\",\n      \"description\": \"Subscription creation\",\n      \"invoice\": \"in_1KHWwFFrKCGqvoMobs1tOcIo\",\n      \"last_payment_error\": null,\n      \"livemode\": false,\n      \"metadata\": {\n      },\n      \"next_action\": null,\n      \"on_behalf_of\": null,\n      \"payment_method\": null,\n      \"payment_method_options\": {\n        \"card\": {\n          \"installments\": null,\n          \"network\": null,\n          \"request_three_d_secure\": \"automatic\"\n        }\n      },\n      \"payment_method_types\": [\n        \"card\"\n      ],\n      \"processing\": null,\n      \"receipt_email\": null,\n      \"review\": null,\n      \"setup_future_usage\": \"off_session\",\n      \"shipping\": null,\n      \"source\": null,\n      \"statement_descriptor\": null,\n      \"statement_descriptor_suffix\": null,\n      \"status\": \"requires_payment_method\",\n      \"transfer_data\": null,\n      \"transfer_group\": null\n    }\n  },\n  \"livemode\": false,\n  \"pending_webhooks\": 2,\n  \"request\": {\n    \"id\": \"req_8I7obnsHybTkbz\",\n    \"idempotency_key\": \"55532db7-035c-4e3d-b5b4-706e40cb9c61\"\n  },\n  \"type\": \"payment_intent.created\"\n}")

  (verify-signature {:body valid-body
                     :headers {"stripe-signature" valid-signature}})

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
      :middleware [verify-signature-middleware]
      :name :stripe/webhook
      :description (at/get-doc #'webhook)
      :responses {200 {:body {:message string?}}}}]]])

