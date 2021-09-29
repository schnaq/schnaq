(ns schnaq.interface.views.startpage.pricing
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [reitit.frontend.easy :as reititfe]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.pages :as pages]))

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
  (label-builder :pricing.features/business))
(defn- enterprise-features []
  (label-builder :pricing.features/enterprise))

(>defn- add-class-to-feature
  "Takes a list of features and appends a css-class to it for proper styling."
  [feature-list class]
  [(s/coll-of string?) string? :ret (s/tuple string? string?)]
  (for [feature feature-list]
    [feature class]))


;; -----------------------------------------------------------------------------

(defn- price-tag
  "Unify the price-tag design."
  [price per-account?]
  [:<>
   [:span.display-4 price " €"]
   [:span (labels :pricing.units/per-month)]
   (when per-account?
     [:<> [:br] [:span (gstring/format "%s. %s" (labels :pricing.units/per-active-account) (labels :pricing.notes/with-vat))]])])

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
   [:p.h6 [:i.fa-lg.text-primary.pr-2 {:class (fa :check/normal)}]
    (labels :pricing.features/implemented)]
   [:p.h6 [:i.fa-lg.text-muted.pr-2 {:class (fa :check/normal)}]
    (labels :pricing.features/to-be-implemented)]])

(defn- cta-button
  "Component to build the call-to-action button in a tier card."
  [label class fn]
  [:div.text-center.pt-4
   [:a.btn {:class class :href fn} label]])

(defn- tier-card
  "Build a single tier card."
  [title subtitle price description features upcoming-features cta-button options]
  (let [title-label (labels title)]
    [:article.card.shadow-sm.mb-2 options
     [:div.card-body
      [:div {:style {:height "17rem"}}
       [:h3.card-title.text-center title-label]
       [:h6.card-subtitle.mb-3.text-muted.text-center (labels subtitle)]
       [:p.text-center price]
       [:p.card-text.text-justify (labels description)]]
      [:ul.pricing-feature-list
       (for [[feature class] features]
         (with-meta
           [:li.list-group-item
            [:i.mr-2 {:class (str class " " (fa :check/normal))}] feature]
           {:key (gstring/format "feature-list-%s-%s" title (toolbelt/slugify feature))}))
       (for [[feature class] (add-class-to-feature upcoming-features "text-muted")]
         (with-meta
           [:li.list-group-item
            [:i.mr-2 {:class (str class " " (fa :check/normal))}] feature]
           {:key (gstring/format "feature-list-%s-%s" title (toolbelt/slugify feature))}))]
      cta-button]]))

(defn- free-tier-card
  "Display the free tier card."
  []
  [tier-card
   :pricing.free-tier/title :pricing.free-tier/subtitle
   [price-tag 0]
   :pricing.free-tier/description
   (add-class-to-feature (concat (starter-features) [(labels :pricing.free-tier/for-free)]) "text-primary")
   nil
   [cta-button (labels :pricing.free-tier/call-to-action) "btn-primary" (reititfe/href :routes.schnaq/create)]])

(defn- business-tier-card
  "Display the business tier card."
  []
  [tier-card
   :pricing.business-tier/title :pricing.business-tier/subtitle
   [price-tag config/pricing-business-tier true]
   :pricing.business-tier/description
   (add-class-to-feature (concat (starter-features) (business-features)) "text-primary")
   (coming-soon)
   [cta-button (labels :pricing.business-tier/call-to-action) "btn-secondary" "mailto:info@schnaq.com"]
   {:class "border-primary shadow-lg"}])

(defn- enterprise-tier-card
  "Show the enterprise tier card."
  []
  [tier-card
   :pricing.enterprise-tier/title :pricing.enterprise-tier/subtitle
   [:span.display-5 (labels :pricing.enterprise-tier/on-request)]
   :pricing.enterprise-tier/description
   (add-class-to-feature (concat (starter-features) (business-features) (enterprise-features)) "text-primary")
   (coming-soon)
   [cta-button (labels :pricing.enterprise-tier/call-to-action) "btn-primary" "mailto:info@schnaq.com"]])

(defn- tier-cards []
  (let [classes "col-12 col-sm-6 col-lg-4"]
    [:section.row
     [:div {:class classes}
      [free-tier-card]
      [:p.p-2.text-muted (labels :pricing.free-tier/beta-notice)]
      [mark-explanation]]
     [:div {:class classes} [business-tier-card]]
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
  [:p.text-dark-blue.display-6.text-center.pt-2
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
  "Question, which are asked often and alleviate fears of subscribing."
  []
  [:div.py-5
   [:h2.text-center.display-5.pb-5 (labels :pricing.faq/heading)]
   [:section
    [:h3.text-center.font-weight-bold.text-dark-blue
     (labels :pricing.faq.terminate/heading)]
    [:p.lead.text-center.pb-3 (labels :pricing.faq.terminate/body)]]
   [:section
    [:h3.text-center.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.extra-price/heading)]
    [:p.lead.text-center.pb-3 (labels :pricing.faq.extra-price/body)]]
   [:section
    [:h3.text-center.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.trial-time/heading)]
    [:p.lead.text-center.pb-3 (labels :pricing.faq.trial-time/body)]]
   [:section
    [:h3.text-center.font-weight-bold.text-dark-blue
     (labels :pricing.faq.longer-trial/heading)]
    [:p.lead.text-center.pb-3 (labels :pricing.faq.longer-trial/body)]]
   [:section
    [:h3.text-center.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.privacy/heading)]
    [:p.lead.text-center.pb-3
     (labels :pricing.faq.privacy/body-1)
     [:a {:href (reititfe/href :routes/privacy)} (labels :pricing.faq.privacy/body-2)]
     (labels :pricing.faq.privacy/body-3)]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading (labels :pricing/headline)
    :page/vertical-header? true}
   [:div.container
    [intro]
    [tier-cards]
    [newsletter]
    [trial-box]
    [schnaq-features]
    [faq]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])
