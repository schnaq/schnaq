(ns schnaq.interface.views.startpage.pricing
  (:require [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.loading :refer [spinner-icon]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.links :as links]))

(defn- label-builder
  "Extract vector from labels and drop the first element, which is always a
  `span` element."
  [label-keyword]
  (rest (labels label-keyword)))

(defn- coming-soon []
  (label-builder :pricing.features/upcoming))
(defn- starter-features []
  (label-builder :pricing.features/free))
(defn- pro-features []
  (label-builder :pricing.features/pro))
(defn- enterprise-features []
  (label-builder :pricing.features/enterprise))

(>defn- add-class-to-feature
  "Takes a list of features and appends a css-class to it for proper styling."
  [feature-list class]
  [(? (s/coll-of string?)) string? :ret (s/coll-of (s/tuple string? string?))]
  (for [feature feature-list]
    [feature class]))

;; -----------------------------------------------------------------------------

(defn- discount-for-choosing-yearly
  "Calculating the discount if user chooses the yearly subscription."
  []
  (let [yearly? @(rf/subscribe [:pricing.interval/yearly?])
        price-monthly (:cost @(rf/subscribe [:pricing.pro/monthly]))
        price-yearly (:cost @(rf/subscribe [:pricing.pro/yearly]))
        discount (- (* (/ (/ price-yearly 12) price-monthly) 100) 100)]
    (when (and price-monthly price-yearly)
      [:span.badge.badge-pill.badge-success.ml-1
       {:class (when-not yearly? "text-muted")}
       (gstring/format "%.0f %" discount)])))

(defn- toggle-payment-period
  "Show toggle to switch between monthly and yearly payment."
  []
  (let [yearly? @(rf/subscribe [:pricing.interval/yearly?])]
    [:div.d-flex.flex-row.pb-3
     [:div.pr-2 {:class (when yearly? "text-muted")}
      (labels :pricing.schnaq.pro.monthly/payment-method)]
     [:div.custom-control.custom-switch
      [:input#subscription-switch.custom-control-input
       {:type :checkbox :checked yearly?
        :on-change #(rf/dispatch [:pricing.interval/toggle-yearly])}]
      [:label.custom-control-label {:for "subscription-switch"}]]
     [:div {:class (when-not yearly? "text-muted")}
      (labels :pricing.schnaq.pro.yearly/payment-method) [discount-for-choosing-yearly]]]))

(defn- price-tag-pro-tier
  "Price tag for pro tier."
  []
  (let [pro-price @(rf/subscribe [:pricing/pro-tier])
        yearly? @(rf/subscribe [:pricing.interval/yearly?])
        formatted-price (if (js/Number.isInteger pro-price) "%d €" "%.2f €")]
    (if (and pro-price (not (zero? pro-price)))
      [:<>
       [:span.display-5 (gstring/format formatted-price pro-price)]
       [:span (labels :pricing.units/per-month)]
       [:p
        (labels :pricing.notes/with-vat)
        ", "
        (labels (if yearly? :pricing.schnaq.pro.yearly/cancel-period :pricing.schnaq.pro.monthly/cancel-period))]]
      [spinner-icon])))

(defn- price-tag-free-tier
  "Price tag for free tier."
  []
  [:<>
   [:span.display-5 "0 €"]
   [:span (labels :pricing.units/per-month)]])

(defn- intro
  "Welcome new users to the pricing page."
  []
  [:section.text-center.pb-5
   [:h3 (labels :pricing.intro/heading)]])

(defn- mark-explanation
  "Explain the check marks."
  []
  [:section.pl-4.pt-2
   [:p.h6 [icon :check/normal "text-primary pr-2" {:size "lg"}]
    (labels :pricing.features/implemented)]
   [:p.h6 [icon :check/normal "text-muted pr-2" {:size "lg"}]
    (labels :pricing.features/to-be-implemented)]])

(defn- cta-button
  "Component to build the call-to-action button in a tier card."
  [label class fn]
  [:div.text-center.py-4
   [:a.btn {:class class :href fn} label]])

(defn- tier-card
  "Build a single tier card."
  [title subtitle icon-name price description features upcoming-features cta-button options]
  (let [title-label (labels title)]
    [:article.card.shadow-sm.mb-2 options
     [:div.card-body
      [:div.card-infos
       [:h3.card-title.text-center title-label]
       [:h6.card-subtitle.mb-3.text-muted.text-center (labels subtitle)]
       [:p.card-text.text-center [icon icon-name "text-primary" {:size "4x"}]]
       [:div.text-center.pb-2 price]
       [:p.card-text.text-justify (labels description)]]
      cta-button
      [:ul.pricing-feature-list
       (for [[feature class] features]
         (with-meta
           [:li.list-group-item
            [icon :check/normal (str class " mr-2")] feature]
           {:key (gstring/format "feature-list-%s-%s" title (toolbelt/slugify feature))}))
       (for [[feature class] (add-class-to-feature upcoming-features "text-muted")]
         (with-meta
           [:li.list-group-item
            [icon :check/normal (str class " mr-2")] feature]
           {:key (gstring/format "feature-list-%s-%s" title (toolbelt/slugify feature))}))]]]))

(defn- free-tier-card
  "Display the free tier card."
  []
  [tier-card
   :pricing.free-tier/title :pricing.free-tier/subtitle :rocket
   [price-tag-free-tier]
   :pricing.free-tier/description
   (add-class-to-feature
    (concat
     [(gstring/format (labels :pricing.features/number-of-users) 100)]
     (starter-features)
     [(labels :pricing.free-tier/for-free)])
    "text-primary")
   nil
   [cta-button (labels :pricing.free-tier/call-to-action) "btn-primary" (rfe/href :routes.schnaq/create)]])

(defn- pro-tier-card
  "Display the pro tier card."
  []
  [tier-card
   :pricing.pro-tier/title :pricing.pro-tier/subtitle :crown
   [price-tag-pro-tier]
   :pricing.pro-tier/description
   (add-class-to-feature
    (concat
     [(gstring/format (labels :pricing.features/number-of-users) 300)]
     (starter-features)
     (pro-features))
    "text-primary")
   (coming-soon)
   (let [authenticated? @(rf/subscribe [:user/authenticated?])
         pro-user? @(rf/subscribe [:user/pro-user?])
         yearly? @(rf/subscribe [:pricing.interval/yearly?])
         price-id (:id @(rf/subscribe [(if yearly? :pricing.pro/yearly :pricing.pro/monthly)]))]
     (if pro-user?
       [:div.alert.alert-info.text-center
        [:p (labels :pricing.pro-tier/already-subscribed)]
        [buttons/anchor (labels :pricing.pro-tier/go-to-settings) (rfe/href :routes.user.manage/account) "btn-outline-dark btn-sm"]]
       [:div.text-center.py-4
        [buttons/button
         (labels :pricing.pro-tier/call-to-action)
         #(if authenticated?
            (rf/dispatch [:subscription/create-checkout-session price-id])
            (rf/dispatch [:keycloak/login (links/checkout-link price-id)]))
         "btn-secondary btn-lg"
         (when-not shared-config/stripe-enabled? {:disabled true})]]))
   {:class "border-primary shadow-lg"}])

(defn- enterprise-tier-card
  "Show the enterprise tier card."
  []
  [tier-card
   :pricing.enterprise-tier/title :pricing.enterprise-tier/subtitle :building
   [:span.display-5 (labels :pricing.enterprise-tier/on-request)]
   :pricing.enterprise-tier/description
   (add-class-to-feature
    (concat
     [(labels :pricing.features.number-of-users/unlimited)]
     (starter-features)
     (pro-features)
     (enterprise-features))
    "text-primary")
   (coming-soon)
   [cta-button (labels :pricing.enterprise-tier/call-to-action) "btn-primary" "mailto:info@schnaq.com"]])

(defn- tier-cards []
  (let [classes "col-12 col-lg-4"]
    [:section.row
     [:div {:class classes}
      [free-tier-card]
      [:p.p-2.text-muted (labels :pricing.free-tier/beta-notice)]
      [mark-explanation]]
     [:div {:class classes} [pro-tier-card]]
     [:div {:class classes} [enterprise-tier-card]]]))

(defn- feature-card
  [title description]
  [:div.card.text-center.feature-card.shadow-sm.mb-1
   [:p.card-text.font-weight-bold title]
   [:p description]])

(defn- faq
  "A taste of the most burning questions of the user answered by our live Q&A."
  []
  [:div.pb-5
   [:span.text-center
    [:h2 (labels :startpage.faq/title)]
    [:p.lead (labels :startpage.faq/subtitle)]]
   [qanda/question-field-and-search-results :light]])

(defn- subscription-information []
  [:div.text-center.text-muted
   [:p (labels :pricing.billing/info-1)]
   [:p (labels :pricing.billing/info-2)]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading (labels :pricing/headline)
    :page/vertical-header? true}
   [:<>
    [:div.container
     [intro]
     [toggle-payment-period]
     [tier-cards]
     [subscription-information]]
    [:div.container-fluid.pt-5
     [faq]]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])


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
 :pricing.pro/monthly
 (fn [db]
   (get-in db [:pricing :schnaq.pro/monthly])))

(rf/reg-sub
 :pricing.pro/yearly
 (fn [db]
   (get-in db [:pricing :schnaq.pro/yearly])))

(rf/reg-sub
 :pricing/pro-tier
 :<- [:pricing.interval/yearly?]
 :<- [:pricing.pro/yearly]
 :<- [:pricing.pro/monthly]
 (fn [[yearly? price-yearly price-monthly]]
   (if yearly?
     (/ (:cost price-yearly) 12)
     (:cost price-monthly))))

(rf/reg-event-db
 :pricing.interval/toggle-yearly
 (fn [db]
   (let [yearly-path [:pricing :yearly?]]
     (if (nil? (get-in db yearly-path))
       (assoc-in db yearly-path false)
       (update-in db yearly-path not)))))

(rf/reg-sub
 :pricing.interval/yearly?
 (fn [db]
   (get-in db [:pricing :yearly?] true)))
