(ns schnaq.interface.views.startpage.pricing
  (:require [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.config :as config]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.loading :refer [spinner-icon]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.qa.inputs :as qanda]
            [schnaq.links :as links]
            [schnaq.shared-toolbelt :as toolbelt]))

(defn- label-builder
  "Extract vector from labels and drop the first element, which is always a
  `span` element."
  [label-keyword]
  (rest (labels label-keyword)))

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

(defn price-tag-pro-tier
  "Price tag for pro tier."
  [price-class]
  (let [pro-price @(rf/subscribe [:pricing/pro-tier])
        currency-symbol @(rf/subscribe [:user.currency/symbol])
        formatted-price (if (.isInteger js/Number pro-price) "%d %s" "%.2f %s")]
    (if (and pro-price (not (zero? pro-price)))
      [:<>
       [:span {:class price-class} (gstring/format formatted-price pro-price currency-symbol)]
       [:span (labels :pricing.units/per-month)]
       [:p
        (labels :pricing.notes/with-vat)
        ", "
        (labels :pricing.schnaq.pro.yearly/cancel-period)]]
      [spinner-icon])))

(defn price-tag-free-tier
  "Price tag for free tier."
  []
  (let [currency-symbol @(rf/subscribe [:user.currency/symbol])]
    [:<>
     [:span.display-5 (gstring/format "0 %s" currency-symbol)]
     [:span (labels :pricing.units/per-month)]]))

(defn- intro
  "Welcome new users to the pricing page."
  []
  [:section.text-center.pb-5
   [:h3 (labels :pricing.intro/heading)]])

(defn- cta-button
  "Component to build the call-to-action button in a tier card."
  [label class fn]
  [:div.text-center.py-4
   [:a.btn {:class class :href fn} label]])

(defn- tier-card
  "Build a single tier card."
  [title subtitle icon-name price description features cta-button options]
  (let [title-label (labels title)]
    [:article.card.shadow-sm.mb-2.h-100 options
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
            [icon :check/normal (str class " me-2")] feature]
           {:key (gstring/format "feature-list-%s-%s" title (toolbelt/slugify feature))}))]]]))

(defn- free-tier-cta-button
  "Button to register a free account."
  []
  [cta-button
   (if @(rf/subscribe [:user/authenticated?])
     [:<>
      (when-not @(rf/subscribe [:user/pro-user?])
        [:span.small (labels :pricing.free-tier/call-to-action-preamble) [:br]])
      (labels :pricing.free-tier/call-to-action-registered)]
     (labels :pricing.free-tier/call-to-action))
   "btn-primary"
   (navigation/href :routes.schnaq/create)])

(defn- free-tier-card
  "Display the free tier card."
  []
  [tier-card
   :pricing.free-tier/title :pricing.free-tier/subtitle :rocket
   [price-tag-free-tier]
   :pricing.free-tier/description
   (add-class-to-feature
    (concat
     [(gstring/format (labels :pricing.features/number-of-users) config/max-concurrent-users-free-tier)]
     (starter-features)
     [(labels :pricing.free-tier/for-free)])
    "text-primary")
   [free-tier-cta-button]])

(defn pro-tier-cta-button
  "Show button to checkout pro."
  []
  (let [authenticated? @(rf/subscribe [:user/authenticated?])
        price-id (:id @(rf/subscribe [:pricing.pro/yearly]))]
    [buttons/button
     [:<> [icon :star "me-2"] (labels :pricing.pro-tier/call-to-action)]
     #(if authenticated?
        (rf/dispatch [:subscription/create-checkout-session price-id])
        (rf/dispatch [:keycloak/login (links/checkout-link price-id)]))
     "btn-secondary btn-lg"]))

(defn- pro-tier-card
  "Display the pro tier card."
  []
  [tier-card
   :pricing.pro-tier/title :pricing.pro-tier/subtitle :crown
   [price-tag-pro-tier "display-5"]
   :pricing.pro-tier/description
   (add-class-to-feature
    (concat
     [(labels :pricing.features/from-previous)]
     [(gstring/format (labels :pricing.features/number-of-users) config/max-concurrent-users-pro-tier)]
     (pro-features))
    "text-primary")
   (if @(rf/subscribe [:user/pro-user?])
     [:div.alert.alert-info.text-center
      [:p (labels :pricing.pro-tier/already-subscribed)]
      [buttons/anchor (labels :pricing.pro-tier/go-to-settings) (navigation/href :routes.user.manage/account) "btn-outline-dark btn-sm"]]
     [:div.text-center.py-4
      [pro-tier-cta-button]])
   {:class "border-primary shadow-lg"}])

(defn enterprise-cta-button
  "Show enterprise inquiry button."
  []
  [:a.btn.btn-primary
   {:href "mailto:info@schnaq.com"
    :on-click #(matomo/track-event "Lead" "Mail-Request" "Enterprise-Plan" 50)}
   (labels :pricing.enterprise-tier/call-to-action)])

(defn- enterprise-tier-card
  "Show the enterprise tier card."
  []
  [tier-card
   :pricing.enterprise-tier/title :pricing.enterprise-tier/subtitle :building
   [:span.display-5 (labels :pricing.enterprise-tier/on-request)]
   :pricing.enterprise-tier/description
   (add-class-to-feature
    (concat
     [(labels :pricing.features/from-previous)]
     [(labels :pricing.features.number-of-users/unlimited)]
     (enterprise-features))
    "text-primary")
   [:div.text-center.py-4 [enterprise-cta-button]]])

(defn- tier-cards []
  (let [classes "col-12 col-lg-4"]
    [:section.row
     [:div {:class classes}
      [:div [free-tier-card]
       [:p.p-2.text-muted (labels :pricing.free-tier/beta-notice)]]]
     [:div {:class classes} [pro-tier-card]]
     [:div {:class classes} [enterprise-tier-card]]]))

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
   [:p (labels :pricing.billing/info-2)]
   [:p (labels :pricing.billing/info-3-edu) " "
    [:a {:href "mailto:hello@schnaq.com"} "hello@schnaq.com"]]
   [:p.small (labels :pricing.billing/info-4-one-time)]])

(defn one-time-information [smaller?]
  [:div.text-center.pt-3 {:class (if smaller? "" "fs-4")}
   [:p (labels :pricing.one-time/question)]
   [:p (gstring/format (labels :pricing.one-time/offer) config/max-concurrent-users-event-tier config/price-event-tier-euro)]
   [:p
    (labels :pricing.one-time/contact) " "
    [:a {:href "mailto:hello@schnaq.com"} "hello@schnaq.com"]]])

(defn- feature-row
  "A single row for the features of the different schnaq plans."
  [feature-labels-ns free pro]
  [:tr
   [:td.align-middle (labels (keyword (name feature-labels-ns) "name"))
    [tooltip/text
     (labels (keyword feature-labels-ns "description"))
     [:span [icon :info-question "small ms-1" {:style {:cursor :help}}]]
     {:placement "right"
      :theme "dark"}]]
   [:td.text-center.align-middle free]
   [:td.text-center.align-middle pro]])

(defn- feature-group
  "A subheading in a table grouping multiple features."
  [group]
  [:tr
   [:td.align-middle.table-transparent.pt-4
    [:span.fw-bold.fs-5 (labels group)]]])

(defonce no-feature [icon :cross "text-warning"])
(defonce feature-included [icon :check/normal "text-primary"])

(defn- feature-details
  "A table displaying all features in detail."
  []
  [:div.table-responsive.pricing-table
   [:table.table.table-striped.table-borderless
    [:thead
     [:tr
      [:th ""]
      [:th.text-center (labels :pricing.table.plans/free)]
      [:th.text-center (labels :pricing.table.plans/pro)]]]
    [:tbody
     [feature-group :pricing.table.core/heading]
     [feature-row :pricing.table.core.schnaqs "10" (labels :pricing.table.number/infinite)]
     [feature-row :pricing.table.core.participants "100" "250"]
     [feature-row :pricing.table.core.additional no-feature (labels :pricing.table.contact/sales)]
     [feature-row :pricing.table.core.activations "1" (labels :pricing.table.number/infinite)]
     [feature-group :pricing.table.qa/heading]
     [feature-row :pricing.table.qa.intelligent-qa "50" (labels :pricing.table.number/infinite)]
     [feature-row :pricing.table.qa.discussions "50" (labels :pricing.table.number/infinite)]
     [feature-row :pricing.table.qa.moderation no-feature feature-included]
     [feature-row :pricing.table.qa.answers feature-included feature-included]
     [feature-row :pricing.table.qa.automatic-answers feature-included feature-included]
     [feature-row :pricing.table.qa.mindmaps feature-included feature-included]
     [feature-group :pricing.table.interaction/heading]
     [feature-row :pricing.table.interaction.polls "1" (labels :pricing.table.number/infinite)]
     [feature-row :pricing.table.interaction.activation feature-included feature-included]
     [feature-row :pricing.table.interaction.word-cloud no-feature feature-included]
     [feature-row :pricing.table.interaction.rankings no-feature feature-included]
     [feature-group :pricing.table.security/heading]
     [feature-row :pricing.table.security.gdrp feature-included feature-included]
     [feature-row :pricing.table.security.germany feature-included feature-included]
     [feature-row :pricing.table.security.anon feature-included feature-included]
     [feature-row :pricing.table.security.code feature-included feature-included]
     [feature-group :pricing.table.advanced/heading]
     [feature-row :pricing.table.advanced.theming no-feature feature-included]
     [feature-row :pricing.table.advanced.analytics no-feature feature-included]
     [feature-row :pricing.table.advanced.summary no-feature feature-included]
     [feature-row :pricing.table.advanced.moderation no-feature feature-included]
     [feature-group :pricing.table.support/heading]
     [feature-row :pricing.table.support.mail feature-included feature-included]
     [feature-row :pricing.table.support.priority no-feature feature-included]]
    [:tfoot
     [:tr
      [:th ""] [:th [free-tier-cta-button]] [:th [pro-tier-cta-button]]]]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading (labels :pricing/headline)
    :page/title (labels :pricing/title)
    :page/description (labels :pricing/description)
    :page/vertical-header? true}
   [:<>
    [:div.container
     [intro]
     [tier-cards]
     [one-time-information]
     [feature-details]]
    [:div.container-fluid.pt-5
     [faq]]
    [:div.container
     [subscription-information]]]])

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
 :pricing.pro/yearly
 (fn [db [_ currency]]
   (let [cur (or currency (get-in db [:user :currency] :eur))]
     (get-in db [:pricing cur :schnaq.pro/yearly]))))

(rf/reg-sub
 :pricing/pro-tier
 :<- [:pricing.pro/yearly]
 (fn [price-yearly]
   (/ (:cost price-yearly) 12)))
