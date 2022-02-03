(ns schnaq.config.stripe)

;; Price of Pro Tier
(def ^:private stripe-price-id-monthly-schnaq-pro
  (or (System/getenv "STRIPE_PRICE_PRO_MONTHLY_ID") "***REMOVED***"))

(def ^:private stripe-price-id-yearly-schnaq-pro
  (or (System/getenv "STRIPE_PRICE_PRO_YEARLY_ID") "***REMOVED***"))

(def prices
  "Lookup prices from stripe."
  {:schnaq.pro/monthly stripe-price-id-monthly-schnaq-pro
   :schnaq.pro/yearly stripe-price-id-yearly-schnaq-pro})
