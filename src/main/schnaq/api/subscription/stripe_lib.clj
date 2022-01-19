(ns schnaq.api.subscription.stripe-lib
  (:require [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [schnaq.api.toolbelt :as at]
            [taoensso.timbre :as log])
  (:import [com.stripe.exception InvalidRequestException]
           [com.stripe.model Price]))

(>defn retrieve-price
  "Query current price via stripe's api."
  [price-id]
  [:stripe.price/id => (? :stripe/price)]
  (try
    (let [price (Price/retrieve price-id)]
      (if (.getActive price)
        {:id price-id
         :cost (-> price .getUnitAmount (/ 100) float)}
        (do (log/error "Queried article is not active:" price-id)
            (at/build-error-body
             :stripe.price/inactive
             (format "Queried article is not active: %s" price-id)))))
    (catch InvalidRequestException _
      (log/error "Price could not be found:" price-id)
      (at/build-error-body
       :stripe.price/invalid-request
       (format "Request could not be fulfilled. Maybe the queried price is not available: %s" price-id)))))
