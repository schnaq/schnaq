(ns schnaq.api.subscription
  (:require [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok forbidden not-found bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException]
           [com.stripe.model Price]
           [com.stripe.model.checkout Session]))

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

(def sample-payload
  {:request {:idempotency_key "c3f22fa4-28f4-42c0-a534-ed7c24dc8dc5", :id "req_oghFbDE3aNtgul"}
   :type "payment_intent.created"
   :created 1640270702
   :pending_webhooks 1
   :id "evt_3K9sXSFrKCGqvoMo0sWX8MwM"
   :api_version "2020-08-27"
   :livemode false
   :object "event"
   :data
   {:object
    {:description "Subscription creation"
     :amount 699
     :canceled_at nil
     :payment_method nil
     :amount_capturable 0
     :processing nil
     :capture_method "automatic"
     :application_fee_amount nil
     :application nil
     :automatic_payment_methods nil
     :setup_future_usage "off_session"
     :statement_descriptor_suffix nil
     :receipt_email nil
     :charges
     {:total_count 0
      :url "/v1/charges?payment_intent=pi_3K9sXSFrKCGqvoMo0nca4k9h"
      :has_more false
      :object "list"
      :data []}
     :on_behalf_of nil
     :created 1640270702
     :payment_method_types ["card" "sepa_debit"]
     :source nil
     :customer "cus_KpXcTOgIYp7Rac"
     :amount_received 0
     :transfer_group nil
     :invoice "in_1K9sXRFrKCGqvoMoXuhbx0a3"
     :cancellation_reason nil
     :currency "eur"
     :confirmation_method "automatic"
     :review nil
     :next_action nil
     :status "requires_payment_method"
     :id "pi_3K9sXSFrKCGqvoMo0nca4k9h"
     :transfer_data nil
     :last_payment_error nil
     :livemode false
     :shipping nil
     :metadata {}
     :object "payment_intent"
     :client_secret "pi_3K9sXSFrKCGqvoMo0nca4k9h_secret_lxLF5I3SX023mv4KepUBCRVVx"
     :statement_descriptor nil
     :payment_method_options
     {:card {:installments nil, :request_three_d_secure "automatic", :network nil}, :sepa_debit {}}}}})

(defn- webhook
  "TODO"
  [{:keys [headers body-params] :as request}]
  (prn request)
  (def foor request)
  (ok {:status :toll}))

(comment

  (:parameters foor)

  nil)

;; -----------------------------------------------------------------------------


(def subscription-routes
  [["/subscription" {:swagger {:tags ["subscription"]}}
    ["" {:middleware [:security/schnaq-csrf-header]}
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
      :description (at/get-doc #'webhook)
      :parameters {:body {:type keyword?
                          :created number?
                          :data {:object {:id string?}}}}}]]])

