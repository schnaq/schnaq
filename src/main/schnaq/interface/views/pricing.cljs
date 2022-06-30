(ns schnaq.interface.views.pricing
  (:require [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.loading :refer [spinner-icon]]))

;; -----------------------------------------------------------------------------

(defn price-tag-pro-tier
  "Price tag for pro tier."
  [price-class]
  (let [pro-price @(rf/subscribe [:pricing/pro-tier])
        currency-symbol @(rf/subscribe [:user.currency/symbol])
        formatted-price (if (.isInteger js/Number pro-price) "%d %s" "%.2f %s")]
    (if (and pro-price (not (zero? pro-price)))
      [:<>
       [:p.mb-0 {:class price-class} (gstring/format formatted-price pro-price currency-symbol)]
       [:span (labels :pricing.units/per-month)]
       [:p
        (labels :pricing.notes/with-vat)
        ", "
        (labels :pricing.schnaq.pro.yearly/cancel-period)]]
      [spinner-icon])))

(defn enterprise-cta-button
  "Show enterprise inquiry button."
  []
  [:a.btn.btn-primary
   {:href "mailto:info@schnaq.com"
    :on-click #(matomo/track-event "Lead" "Mail-Request" "Enterprise-Plan" 50)}
   (labels :pricing.enterprise-tier/call-to-action)])

(defn one-time-information [smaller?]
  [:div.text-center.pt-3 {:class (if smaller? "" "fs-4")}
   [:p (labels :pricing.one-time/question)]
   [:p (gstring/format (labels :pricing.one-time/offer) config/max-concurrent-users-pro-tier config/price-event-tier-euro)]
   [:p
    (labels :pricing.one-time/contact) " "
    [:a {:href "mailto:hello@schnaq.com"} "hello@schnaq.com"]]])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :subscription/create-checkout-session
 (fn [{:keys [db]} [_ price-id]]
   {:fx [(http/xhrio-request db :get "/stripe/create-checkout-session"
                             [:navigation.redirect/follow]
                             {:price-id price-id})]}))

(rf/reg-event-fx
 :pricing/get-prices
 (fn [{:keys [db]}]
   {:fx [(http/xhrio-request db :get "/stripe/prices"
                             [:pricing/store-prices])]}))

(rf/reg-event-db
 :pricing/store-prices
 (fn [db [_ {:keys [prices]}]]
   (update db :pricing merge prices)))

(rf/reg-sub
 :pricing.pro/yearly
 (fn [db [_ currency]]
   (let [cur (or currency (get-in db [:user :currency] :eur))]
     (get-in db [:pricing cur :schnaq.pro/yearly]))))

(rf/reg-sub
 :pricing/pro-tier
 :<- [:pricing.pro/yearly]
 (fn [price-yearly]
   (/ (:cost price-yearly) 12)))
