(ns schnaq.interface.views.startpage.pricing
  (:require [schnaq.interface.views.pages :as pages]))

(defn- free-tier-card
  []
  [:div.card.shadow-sm
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
  [:div.card.shadow-sm
   [:div.card-body.d-flex.flex-column
    [:h3.card-title.text-center "Business"]
    [:p.card-text "Ob 10 oder 50 Nutzer:innen - der Preis ist der gleiche.
      Eignet sich für Unternehmen, Vereine, Bildungsinstitutionen und alle
      die strukturiert Wissen sammeln möchten."]
    [:p.card-text.text-center [:span.display-2 "99 €"] [:span "/ Monat"]]
    [:p.text-muted.text-center "zzgl. MwSt."]
    [:p.text-muted.text-center "Bei jährlicher Zahlweise im Voraus 15% Rabatt"]
    [:div.text-center.mt-auto
     [:button.btn.button-primary {:disabled true}
      [:p.card-text "Verfügbar ab 01.01.2021"]]]]])

(defn- trial-box
  []
  [:div.d-flex.justify-content-center.py-4
   [:div.trial-box.text-center.button-dark
    [:p.display-6.font-weight-bold "30 Tage Business testen"]
    [:p "Keine Kreditkarte nötig! Jederzeit kündbar."]
    [:p.text-sm.text-muted "Verfügbar ab 01.01.2021"]]])

(defn- pricing-box
  "A box displaying the different subscription tiers we offer."
  []
  [:div.card-deck
   [free-tier-card]
   [business-tier-card]])

(defn- feature-card
  [title description]
  [:div.card.text-center.feature-card.shadow-sm
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
    [feature-card "Priority Support" "Ihre Fragen und anliegen wandern sofort an die Spitze der Liste. *"]
    [feature-card "Interaktive Mindmap" "Alle Beiträge werden automatisch graphisch und interaktiv dargestellt."]]
   [:p.text-sm.text-muted "* Gilt nur für Business-Abonnement"]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading "Einmal schnaqqen, alles wissen!"
    :page/subheading "Schnaq Abonnement"}
   [:div.container
    [pricing-box]
    [trial-box]
    [schnaq-features]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])