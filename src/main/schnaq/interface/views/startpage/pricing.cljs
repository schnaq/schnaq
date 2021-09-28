(ns schnaq.interface.views.startpage.pricing
  (:require [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [reitit.frontend.easy :as reititfe]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [cljs.spec.alpha :as s]))

(def ^:private coming-soon
  ["K.I. Stimmungsanalyse"
   "Sprache-zu-Text"])

(def ^:private starter-features
  ["In Deutschland gehostet"
   "Diskussionen erstellen"
   "Automatische Mindmap"
   "Teilbar per Link"
   "Text- und Bild-Export"])

(def ^:private paid-features
  ["Analyse-Dashboard"
   "K.I. Zusammenfassungen"
   "Persönlicher Bereich"])

(def ^:private enterprise-features
  ["Einbettung in bestehende Systeme"
   "SSO Login (OpenID, LDAP, ...)"
   "Whitelabeling"
   "On-Premise"])

(>defn- add-class-to-feature
  [feature-list class]
  [(s/coll-of string?) string? :ret (s/tuple string? string?)]
  (for [feature feature-list]
    [feature class]))

(defn- build-feature-list-items [title [feature class]]
  (with-meta
    [:li.list-group-item
     [:i.mr-2 {:class (str class " " (fa :check/normal))}] feature]
    {:key (gstring/format "feature-list-%s-%s" title feature)}))


;; -----------------------------------------------------------------------------

(defn- card [title subtitle price description features upcoming-features options]
  [:article.card.shadow-sm.mb-2 options
   [:div.card-body
    [:div {:style {:height "17rem"}}
     [:h3.card-title.text-center title]
     [:h6.card-subtitle.mb-3.text-muted.text-center subtitle]
     [:p.text-center price]
     [:p.card-text.text-justify description]]
    [:ul.pricing-feature-list
     (for [feature features]
       [build-feature-list-items title feature])
     (for [feature (add-class-to-feature upcoming-features "text-muted")]
       [build-feature-list-items title feature])]
    [:a.card-link {:href "#"} "Card link"]]])

(defn- price-tag [price per-account?]
  [:<>
   [:span.display-4 price " €"]
   [:span (labels :pricing.units/per-month)]
   (when per-account?
     [:p "pro aktivem Account"])])

(defn- intro
  "Welcome new users to the pricing page."
  []
  [:section.text-center.pb-5
   [:h2 (labels :pricing.intro/heading)]
   [:p.lead (labels :pricing.intro/lead)]])

(defn- mark-explanation []
  [:section.pl-4.pt-2
   [:p.h6 [:i.fa-lg.text-primary.pr-2 {:class (fa :check/normal)}] "Bereits implementiert"]
   [:p.h6 [:i.fa-lg.text-muted.pr-2 {:class (fa :check/normal)}] "Bald verfügbar"]])

(defn- pricings []
  (let [classes "col-12 col-sm-6 col-lg-4"]
    [:section.row
     [:div {:class classes}
      [card "Starter" "Individuell" [price-tag 0] "Starte direkt mit deinen eigenen Diskussionen!" (add-class-to-feature (conj starter-features "Dauerhaft kostenfrei") "text-primary") nil]
      [mark-explanation]]
     [:div {:class classes}
      [card "Business" "Bring dein Team zusammen" [price-tag 6 true] "Lasse dich von unserer K.I. unterstützen und erfahre mehr zu deinen Diskussionen!" (add-class-to-feature (concat starter-features paid-features) "text-primary") coming-soon {:class "border-primary shadow-lg"}]]
     [:div {:class classes}
      [card "Enterprise" "Für deine Institution" [:span.display-5 "Auf Anfrage"] "Möchtest du deine gesamte Firma / Institution / Universität anbinden? Dann bist du hier richtig!" (add-class-to-feature (concat starter-features paid-features enterprise-features) "text-primary") coming-soon]]]))

(defn- free-tier-card
  []
  [:div.card.shadow-sm.tier-card
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Starter"]
    [:p.card-text (labels :pricing.free-tier/description)]
    [:p.card-text.text-center.display-2 "0 €"]
    [:p.text-muted.text-center (labels :pricing.free-tier/beta-notice)]
    [:div.text-center.mt-auto
     [:a.btn.button-primary
      {:href (reititfe/href :routes.schnaq/create)}
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

(defn- check-item
  "List-item with checkmark bullet-point."
  [text]
  [:li [:span.fa-li [:i {:class (fa :check/normal)}]] text])

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
    [pricings]
    [newsletter]
    [trial-box]
    [schnaq-features]
    #_[comparison]
    [faq]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])
