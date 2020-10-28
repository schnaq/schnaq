(ns schnaq.interface.views.startpage.pricing
  (:require [schnaq.interface.views.pages :as pages]))

(defn- pricing-box
  "A box displaying the different subscription tiers we offer."
  []
  [:div.card-deck
   [:div.card.shadow-sm
    [:div.card-body.d-flex.flex-column
     [:h3.card-title.text-center "Gratis"]
     [:p.card-text "Für kleine Teams und private Zwecke. Der gratis Plan ist der
     perfekte Einstieg in strukturierte Wissensgenerierung."]
     [:p.card-text.text-center.display-2 "0 €"]
     [:p.text-muted.text-center "Nach der Beta-Phase ist der Plan weiterhin verfügbar für bis zu 5 Nutzer:innen pro Team"]
     [:div.text-center.mt-auto
      [:button.btn.button-primary
       [:p.card-text "Gratis loslegen"]]]]]
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
       [:p.card-text "Verfügbar ab 01.01.2021"]]]]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading "Einmal schnaqqen, alles wissen!"
    :page/subheading "Schnaq Abonnement"}
   [:section.container
    [pricing-box]]])

(defn pricing-view
  "The pricing view."
  []
  [pricing-page])