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
  (label-builder :pricing.features/starter))
(defn- business-features []
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

(defn- price-tag-pro-tier
  "Price tag for pro tier."
  []
  (if-let [pro-price @(rf/subscribe [:pricing/pro-tier])]
    [:<>
     [:span.display-5 pro-price " €"]
     [:span (labels :pricing.units/per-month)]
     [:br]
     [:small.text-muted (labels :pricing.notes/with-vat)]]
    [spinner-icon]))

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
   [:h2 (labels :pricing.intro/heading)]
   [:p.lead (labels :pricing.intro/lead)]])

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
     (business-features))
    "text-primary")
   (coming-soon)
   (let [authenticated? @(rf/subscribe [:user/authenticated?])
         pro-user? @(rf/subscribe [:user/pro-user?])]
     (if pro-user?
       [:div.alert.alert-info.text-center
        [:p (labels :pricing.pro-tier/already-subscribed)]
        [buttons/anchor (labels :pricing.pro-tier/go-to-settings) (rfe/href :routes.user.manage/account) "btn-outline-dark btn-sm"]]
       [:div.text-center.py-4
        [buttons/button
         (labels :pricing.pro-tier/call-to-action)
         #(if authenticated?
            (rf/dispatch [:subscription/create-checkout-session shared-config/stripe-price-id-schnaq-pro])
            (rf/dispatch [:keycloak/login (links/checkout-link)]))
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
     (business-features)
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

(defn- trial-box
  []
  [:div.d-flex.justify-content-center.py-5
   [:div.trial-box.text-center.button-dark.shadow-sm
    [:p.display-6.font-weight-bold (labels :pricing.trial/call-to-action)]
    [:p (labels :pricing.trial/description)]
    [:p.text-sm.text-muted (labels :pricing.trial.temporary/deactivation)]]])

(defn- newsletter
  "A box displaying the different subscription tiers we offer."
  []
  [:p.text-typography.display-6.text-center.pt-4
   (labels :pricing.newsletter/lead)
   [:a.btn.btn-lg.btn-link
    {:href "https://schnaq.us8.list-manage.com/subscribe?u=adbf5722068bcbcc4c7c14a72&id=407d47335d"}
    (labels :pricing.newsletter/name)]])

(defn- feature-card
  [title description]
  [:div.card.text-center.feature-card.shadow-sm.mb-1
   [:p.card-text.font-weight-bold title]
   [:p description]])

(defn- schnaq-features
  "List all features that are making schnaq a good deal."
  []
  [:div.my-3
   [:h3.text-center (labels :pricing.features/heading)]
   [:div.card-deck
    [feature-card (labels :pricing.features.user-numbers/heading) (labels :pricing.features.user-numbers/content)]
    [feature-card (labels :pricing.features.team-numbers/heading) (labels :pricing.features.team-numbers/content)]
    [feature-card (labels :pricing.features.app-integration/heading) (labels :pricing.features.app-integration/content)]]
   [:div.card-deck.mt-2
    [feature-card (labels :pricing.features.analysis/heading) (labels :pricing.features.analysis/content)]
    [feature-card (labels :pricing.features.knowledge-db/heading) (labels :pricing.features.knowledge-db/content)]
    [feature-card (labels :pricing.features.mindmap/heading) (labels :pricing.features.mindmap/content)]]
   [:p.text-sm.text-muted (labels :pricing.features/disclaimer)]])

(defn- faq
  "A taste of the most burning questions of the user answered by our live Q&A."
  []
  [:div.pb-5
   [:span.text-center
    [:h2 (labels :startpage.faq/title)]
    [:p.lead (labels :startpage.faq/subtitle)]]
   [qanda/question-field-and-search-results :light]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading (labels :pricing/headline)
    :page/vertical-header? true}
   [:<>
    [:div.container
     [intro]
     [tier-cards]
     [newsletter]
     [trial-box]
     [schnaq-features]]
    [:div.container-fluid
     [faq]]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])


;; -----------------------------------------------------------------------------


(rf/reg-event-fx
 :subscription/create-checkout-session
 (fn [{:keys [db]} [_ price-id]]
   {:fx [(http/xhrio-request db :post "/stripe/create-checkout-session"
                             [:navigation.redirect/follow]
                             {:price-id price-id})]}))

(rf/reg-event-fx
 :pricing/get-price
 (fn [{:keys [db]} [_ price-id]]
   {:fx [(http/xhrio-request db :get "/stripe/price"
                             [:pricing/store-price]
                             {:price-id price-id})]}))

(rf/reg-event-db
 :pricing/store-price
 (fn [db [_ {:keys [id cost]}]]
   (assoc-in db [:pricing id] cost)))

(rf/reg-sub
 :pricing/pro-tier
 (fn [db _]
   (get-in db [:pricing shared-config/stripe-price-id-schnaq-pro])))
