(ns schnaq.config.stripe
  "Define the stripe prices. Fallbacks are the test prices."
  (:require [config.core :refer [env]]))

(def prices
  "Store stripe price-ids."
  {:eur
   {:schnaq.pro/yearly (:stripe-price-pro-yearly-id env)}
   :usd
   {:schnaq.pro/yearly (:stripe-price-pro-usd-yearly-id env)}})
