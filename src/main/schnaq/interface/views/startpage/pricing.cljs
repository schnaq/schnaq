(ns schnaq.interface.views.startpage.pricing
  (:require [reitit.frontend.easy :as reititfe]
            [schnaq.interface.text.display-data :refer [img-path fa labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- free-tier-card
  []
  [:div.card.shadow-sm.tier-card
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Starter"]
    [:p.card-text (labels :pricing.free-tier/description)]
    [:p.card-text.text-center.display-2 "0 €"]
    [:p.text-muted.text-center (labels :pricing.free-tier/beta-notice)]
    [:div.text-center.mt-auto
     [:button.btn.button-primary
      [:p.card-text (labels :pricing.free-tier/call-to-action)]]]]])

(defn- business-tier-card
  []
  [:div.card.shadow-sm.tier-card
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Business"]
    [:p.card-text (labels :pricing.business-tier/description)]
    [:p.card-text.text-center [:span.display-2 "79 €"] [:span (labels :pricing.units/per-month)]]
    [:p.text-muted.text-center (labels :pricing.notes/with-vat)]
    [:p.text-muted.text-center (labels :pricing.notes/yearly-rebate)]
    [:div.text-center.mt-auto
     [:button.btn.button-primary {:disabled true}
      [:p.card-text (labels :pricing.business-tier/call-to-action)]]]]])

(defn- trial-box
  []
  [:div.d-flex.justify-content-center.py-5
   [:div.trial-box.text-center.button-dark.shadow-sm
    [:p.display-6.font-weight-bold (labels :pricing.trial/call-to-action)]
    [:p (labels :pricing.trial/description)]
    [:p.text-sm.text-muted (labels :pricing.trial.temporary/deactivation)]]])

(defn- pricing-box
  "A box displaying the different subscription tiers we offer."
  []
  [:<>
   [:div.card-deck.pt-3
    [free-tier-card]
    [business-tier-card]]
   [:p.text-dark-blue.display-6.text-center.pt-2
    (labels :pricing.newsletter/lead)
    [:a {:href "https://disqtec.com/newsletter"} (labels :pricing.newsletter/name)]]])

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

(defn- double-check-item
  "List-item with custom double-check bullet-point."
  [text]
  [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] text])

(defn- competitor-box
  "Comparison box for a competitor."
  [name img-key price description]
  [:div.row.py-2.mb-3.comparison-box.shadow-sm
   [:div.col-3.d-flex
    [:img.img-fluid.pricing-logo.align-self-center {:src (img-path img-key) :alt (str name " logo")}]]
   [:div.col-9
    [:h3 name]
    [:p [:span.display-6 (str price (labels :pricing.competitors/per-month-per-user))] [:br]
     description]]])

(defn- comparison
  "Show that we are cheaper than user-based alternatives. Also drop important search keyword
  'schnaq vergleich <some competitors>'"
  []
  [:div.py-5
   [:h2.text-center.pb-1.display-4 (labels :pricing.comparison/heading)]
   [:h3.text-center.display-6 (labels :pricing.comparison/subheading)]
   [:div.row.pt-4.d-flex.mx-1.mx-lg-0
    [:div.col-12.col-lg-5.p-0
     [:div.row.comparison-box.shadow-sm.p-2
      [:div.col-3.d-flex
       [:img.img-fluid.pricing-logo.align-self-center {:src (img-path :schnaqqifant/original) :alt "schnaq logo"}]]
      [:div.col-9
       [:h3 "schnaq"]
       [:p.display-6 (labels :pricing.comparison.schnaq/price-point)]]
      [:div.col-12.mb-4
       [:hr]
       [:ul.fa-ul.display-6.pricing-checklist
        [double-check-item (labels :pricing.comparison.schnaq/brainstorm)]
        [double-check-item (labels :pricing.comparison.schnaq/decision-making)]
        [double-check-item (labels :pricing.comparison.schnaq/knowledge-db)]
        [double-check-item (labels :pricing.comparison.schnaq/async)]
        [double-check-item (labels :pricing.comparison.schnaq/mindmap)]
        [double-check-item (labels :pricing.comparison.schnaq/analysis)]]]]
     [:div.row.comparison-box.shadow-sm.mt-3
      [:div.col-12
       [:p.text-center.py-2 [:span.display-6 [:span.display-5 "79 €"] (labels :pricing.comparison.schnaq/flatrate)] [:br]
        (labels :pricing.comparison.schnaq/person-20) [:br]
        (labels :pricing.comparison.schnaq/person-50) [:br]
        (labels :pricing.comparison.schnaq/person-100)]]]]
    [:div.col-12.col-lg-2.text-center.align-self-center
     [:p.pricing-vs.font-weight-bold.mt-3 (labels :pricing.comparison/compared-to)]]
    [:div.col-12.col-lg-5.p-0
     [competitor-box "Miro" :pricing.others/miro "6,80" (labels :pricing.comparison.miro/description)]
     [competitor-box "Loomio" :pricing.others/loomio "2,60" (labels :pricing.comparison.loomio/description)]
     [competitor-box "Confluence" :pricing.others/confluence "4,30" (labels :pricing.comparison.confluence/description)]
     [:div.row.comparison-box.shadow-sm.mt-3
      [:div.col-12
       [:p.text-center.py-2 [:span.display-6 [:span.display-5 "137 €"] (labels :pricing.comparison.competitor/person-10)]
        [:br]
        (labels :pricing.comparison.competitor/person-20) [:br]
        (labels :pricing.comparison.competitor/person-50) [:br]
        (labels :pricing.comparison.competitor/person-100)]]]]]])

(defn- faq
  "Question, which are asked often and alleviate fears of subscribing."
  []
  [:div.py-5
   [:h2.text-center.display-4.pb-5 (labels :pricing.faq/heading)]
   [:section
    [:h3.text-center.display-5.font-weight-bold.text-dark-blue
     (labels :pricing.faq.terminate/heading)]
    [:p.display-6.text-center.pb-3 (labels :pricing.faq.terminate/body)]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.extra-price/heading)]
    [:p.display-6.text-center.pb-3 (labels :pricing.faq.extra-price/body)]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.trial-time/heading)]
    [:p.display-6.text-center.pb-3 (labels :pricing.faq.trial-time/body)]]
   [:section
    [:h3.text-center.display-5.font-weight-bold.text-dark-blue
     (labels :pricing.faq.longer-trial/heading)]
    [:p.display-6.text-center.pb-3 (labels :pricing.faq.longer-trial/body)]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     (labels :pricing.faq.privacy/heading)]
    [:p.display-6.text-center.pb-3
     (labels :pricing.faq.privacy/body-1)
     [:a {:href (reititfe/href :routes/privacy)} (labels :pricing.faq.privacy/body-2)]
     (labels :pricing.faq.privacy/body-3)]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading [:span.d-block.text-center (labels :pricing/headline)]}
   [:div.container
    [pricing-box]
    [trial-box]
    [schnaq-features]
    [comparison]
    [faq]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])