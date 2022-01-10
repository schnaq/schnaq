(ns schnaq.api.stripe
  (:require [clojure.pprint :refer [pprint]]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [ok forbidden not-found bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [taoensso.timbre :as log])
  (:import [com.stripe Stripe]
           [com.stripe.exception InvalidRequestException]
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
     "metadata" {:keycloak-id keycloak-id}
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

(defmulti stripe-event
  (fn [event] (:type event)))

(defmethod stripe-event :payment_intent.succeeded [event]
  (def success-event event)
  (log/info "Payment intent successful 🤑"))

(defmethod stripe-event :default [_event])

(defn- webhook
  "TODO"
  [{:keys [headers body-params parameters] :as request}]
  (pprint body-params)
  (stripe-event (:body parameters))
  (def foor request)
  (ok {:status :toll}))

(comment

  (:body foor)
  (def sig (get-in foor [:headers "stripe-signature"]))

  (Webhook/constructEvent (slurp (:body foor)) sig config/stripe-webhook-access-key)

  (:parameters foor)

  nil)

;; -----------------------------------------------------------------------------


(def stripe-routes
  [["/stripe" {:swagger {:tags ["subscription" "stripe"]}}
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

