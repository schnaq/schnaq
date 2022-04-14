(ns schnaq.config.stripe
  "Define the stripe prices. Fallbacks are the test prices.")

(def prices
  "Store stripe price-ids."
  {:eur
   {:schnaq.pro/yearly (or (System/getenv "STRIPE_PRICE_PRO_YEARLY_ID") "***REMOVED***")}
   :usd
   {:schnaq.pro/yearly (or (System/getenv "STRIPE_PRICE_PRO_USD_YEARLY_ID") "***REMOVED***")}})
