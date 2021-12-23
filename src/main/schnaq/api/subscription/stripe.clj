(ns schnaq.api.subscription.stripe
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config])
  (:import [com.stripe Stripe]
           [com.stripe.model.checkout Session]
           [com.stripe.param.checkout SessionCreateParams SessionCreateParams$LineItem SessionCreateParams$Mode]))

(s/def :stripe/line-item (partial instance? SessionCreateParams$LineItem))
(s/def :stripe/session-create-params (partial instance? SessionCreateParams))

(set! (. Stripe -apiKey) config/stripe-secret-api-key)

(>defn- build-line-item
  "Take the current product price id and use stripe's java API to construct
  a line item, which can be added to the checkout session parameters, i.e. this
  is an item in the cart of the customer."
  [product-price-id]
  [string? :ret :stripe/line-item]
  (.. (SessionCreateParams$LineItem/builder)
      (setPrice product-price-id)
      (setQuantity 1)
      (build)))

(defn- build-checkout-session-parameters
  "Build a checkout session, i.e. open stripe's checkout window, with the line-
  items added to the cart."
  [line-item]
  [:stripe/line-item :ret :stripe/session-create-params]
  (.. (SessionCreateParams/builder)
      (addLineItem line-item)
      (setMode SessionCreateParams$Mode/SUBSCRIPTION)
      (setSuccessUrl (format "%s/subscription/success" config/frontend-url))
      (setCancelUrl (format "%s/subscription/cancel" config/frontend-url))
      (build)))

(defn create-checkout-session
  "Open stripe's checkout page with the currently selected item."
  [{{{:keys [product-price-id]} :body} :parameters}]
  (let [line-item (build-line-item product-price-id)
        checkout-session-parameters (build-checkout-session-parameters line-item)
        session (Session/create checkout-session-parameters)]
    (ok {:redirect (.getUrl session)})))

;; -----------------------------------------------------------------------------

(def stripe-routes
  [["/subscription" {:swagger {:tags ["subscription"]}}
    ["/create-checkout-session"
     {:post create-checkout-session
      :description (at/get-doc #'create-checkout-session)
      :parameters {:body {:product-price-id string?}}
      :responses {200 {:body {:redirect string?}}}}]]])
