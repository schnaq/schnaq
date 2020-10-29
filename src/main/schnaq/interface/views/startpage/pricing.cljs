(ns schnaq.interface.views.startpage.pricing
  (:require [schnaq.interface.text.display-data :refer [img-path]]
            [schnaq.interface.views.pages :as pages]))

(defn- free-tier-card
  []
  [:div.card.shadow-sm.tier-card
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Starter"]
    [:p.card-text "Für kleine Teams und private Zwecke. Der Starter Plan ist der
     perfekte Einstieg in strukturierte Wissensgenerierung."]
    [:p.card-text.text-center.display-2 "0 €"]
    [:p.text-muted.text-center "Nach der Beta-Phase ist der Plan weiterhin verfügbar für bis zu 5 Nutzer:innen pro Team"]
    [:div.text-center.mt-auto
     [:button.btn.button-primary
      [:p.card-text "Kostenfrei loslegen"]]]]])

(defn- business-tier-card
  []
  [:div.card.shadow-sm.tier-card
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Business"]
    [:p.card-text "Ob 10 oder 50 Nutzer:innen - der Preis ist der gleiche.
      Eignet sich für Unternehmen, Vereine, Bildungsinstitutionen und alle
      die strukturiert Wissen sammeln möchten."]
    [:p.card-text.text-center [:span.display-2 "79 €"] [:span "/ Monat"]]
    [:p.text-muted.text-center "zzgl. MwSt."]
    [:p.text-muted.text-center "Bei jährlicher Zahlweise im Voraus 15% Rabatt"]
    [:div.text-center.mt-auto
     [:button.btn.button-primary {:disabled true}
      [:p.card-text "Verfügbar ab 01.01.2021"]]]]])

(defn- trial-box
  []
  [:div.d-flex.justify-content-center.py-4
   [:div.trial-box.text-center.button-dark.shadow-sm
    [:p.display-6.font-weight-bold "30 Tage Business testen"]
    [:p "Keine Kreditkarte nötig! Jederzeit kündbar."]
    [:p.text-sm.text-muted "Verfügbar ab 01.01.2021"]]])

(defn- pricing-box
  "A box displaying the different subscription tiers we offer."
  []
  [:div.card-deck.pt-3
   [free-tier-card]
   [business-tier-card]])

(defn- feature-card
  [title description]
  [:div.card.text-center.feature-card.shadow-sm.mb-1
   [:p.card-text.font-weight-bold title]
   [:p description]])

(defn- schnaq-features
  "List all features that are making schnaq a good deal."
  []
  [:div.mt-2
   [:h3.text-center "Schnaq Abonnement Vorteile"]
   [:div.card-deck
    [feature-card "Unbegrenzte Teilnehmer:innen" "Lassen Sie so viele Mitarbeiter:innen wie Sie möchten kooperieren. *"]
    [feature-card "Unbegrenzte Teams" "Die Anzahl der Teams die Sie erstellen können ist unlimitiert. *"]
    [feature-card "App-Integration" "Verknüpfen Sie schnaq leicht mit Ihrem Slack, MS Teams, Confluence …"]]
   [:div.card-deck.mt-2
    [feature-card "Automatische Analysen" "Die Beiträge werden automatisch Analysiert und für alle Teilnehmer:innen aufbereitet."]
    [feature-card "Wissensdatenbank" "Sammeln Sie erarbeitetes Wissen und Ideen an einem Ort."]
    [feature-card "Interaktive Mindmap" "Alle Beiträge werden automatisch graphisch und interaktiv dargestellt."]]
   [:p.text-sm.text-muted "* Gilt nur für Business-Abonnement"]])

;; TODO bei kleineren devices andere rows
(defn- comparison
  "Show that we are cheaper than user-based alternatives. Also drop important search keyword
  'schnaq vergleich <some competitors>'"
  []
  [:div.py-5
   [:h1.text-center.pb-1 "Sie wachsen weiter - Sie sparen mehr!"]
   [:h3.text-center.display-6 "Egal wie groß Ihr Team wird, der Preis bleibt der gleiche.
   So schlägt sich der Preis von schnaq im Vergleich zu Miro + Loomio + Confluence im Paket."]
   [:div.row
    [:div.col-5.ml-auto.comparison-box.shadow-sm
     [:div.row.pt-3
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :schnaqqifant/original) :alt "schnaq logo"}]]
      [:div.col-9
       [:h3 "schnaq"]
       [:p.display-6 "79 € pro Monat für ihr Unternehmen"]]]]
    [:div.col-2.ml-auto.text-center "Vs."]
    [:div.col-5.ml-auto.shadow-sm
     [:div.row.pt-3.comparison-box
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/miro) :alt "miro logo"}]]
      [:div.col-9
       [:h3 "Miro"]
       [:p [:span.display-6 "6,80 € pro Monat pro Nutzer:in"] [:br]
        "Brainstorming Software"]]]
     [:div.row.pt-3.mt-3.comparison-box
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/loomio) :alt "loomio logo"}]]
      [:div.col-9
       [:h3 "Loomio"]
       [:p [:span.display-6 "2,60 € pro Monat pro Nutzer:in"] [:br]
        "Kooperative Entscheidungsfindung"]]]
     [:div.row.pt-3.mt-3.comparison-box
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/confluence) :alt "confluence logo"}]]
      [:div.col-9
       [:h3 "Confluence"]
       [:p [:span.display-6 "4,30 € pro Monat pro Nutzer:in"] [:br]
        "Wissensablage"]]]]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading "Einmal schnaqqen, alles wissen!"
    :page/subheading "Schnaq Abonnement"}
   [:div.container
    [pricing-box]
    [trial-box]
    [schnaq-features]
    [comparison]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])