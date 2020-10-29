(ns schnaq.interface.views.startpage.pricing
  (:require [reitit.frontend.easy :as reititfe]
            [schnaq.interface.text.display-data :refer [img-path fa labels]]
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
    [:p.card-text "Ob 10 oder 50 Nutzer:innen – der Preis ist der gleiche.
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
  [:div.d-flex.justify-content-center.py-5
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
  [:div.my-3
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
;; TODO vs. stylen
(defn- comparison
  "Show that we are cheaper than user-based alternatives. Also drop important search keyword
  'schnaq vergleich <some competitors>'"
  []
  [:div.py-5
   [:h2.text-center.pb-1.display-4 "Sie wachsen weiter – Sie sparen mehr!"]
   [:h3.text-center.display-6 "Egal wie groß Ihr Team wird, der Preis bleibt der Gleiche.
   So schlägt sich der Preis von schnaq im Vergleich zu Miro + Loomio + Confluence im Paket."]
   [:div.row.pt-4.d-flex
    [:div.col-5.ml-auto.comparison-box.shadow-sm
     [:div.row.pt-3
      [:div.col-3.d-flex
       [:img.img-fluid.pricing-logo.align-self-center {:src (img-path :schnaqqifant/original) :alt "schnaq logo"}]]
      [:div.col-9
       [:h3 "schnaq"]
       [:p.display-6 "79 € pro Monat für Ihr Unternehmen"]]]
     [:div.row
      [:div.col-12
       [:hr]
       [:ul.fa-ul.display-6.pricing-checklist
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Brainstorming"]
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Entscheidungsfindung"]
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Wissensdatenbank"]
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Asynchrone Kommunikation"]
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Mindmapping"]
        [:li [:span.fa-li [:i {:class (str "fas " (fa :check-double))}]] "Ergebnisanalyse"]]]]]
    [:div.col-2.ml-auto.text-center.align-self-center
     [:p.pricing-vs.font-weight-bold "Compared" [:br] "to"]]
    [:div.col-5.ml-auto
     [:div.row.pt-3.comparison-box.shadow-sm
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/miro) :alt "miro logo"}]]
      [:div.col-9
       [:h3 "Miro"]
       [:p [:span.display-6 "6,80 € pro Monat pro Nutzer:in"] [:br]
        "Brainstorming Software"]]]
     [:div.row.pt-3.mt-3.comparison-box.shadow-sm
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/loomio) :alt "loomio logo"}]]
      [:div.col-9
       [:h3 "Loomio"]
       [:p [:span.display-6 "2,60 € pro Monat pro Nutzer:in"] [:br]
        "Kooperative Entscheidungsfindung"]]]
     [:div.row.pt-3.mt-3.comparison-box.shadow-sm
      [:div.col-3
       [:img.img-fluid.pricing-logo {:src (img-path :pricing.others/confluence) :alt "confluence logo"}]]
      [:div.col-9
       [:h3 "Confluence"]
       [:p [:span.display-6 "4,30 € pro Monat pro Nutzer:in"] [:br]
        "Wissensablage"]]]]]
   [:div.row.pt-2
    [:div.col-5.comparison-box.shadow-sm
     [:p.text-center.pt-2 [:span.display-6 [:span.display-5 "79 €"] " Flatrate im Monat"] [:br]
      "79 € für 20 Personen" [:br]
      "79 € für 50 Personen" [:br]
      "79 € für 100 Personen …"]]
    [:div.col-5.ml-auto.comparison-box.shadow-sm
     [:p.text-center.pt-2 [:span.display-6 [:span.display-5 "137 €"] " im Monat für 10 Personen"] [:br]
      "247 € für 20 Personen" [:br]
      "685 € für 50 Personen" [:br]
      "1370 € für 100 Personen …"]]]])

(defn- faq
  "Question, which are asked often and alleviate fears of subscribing."
  []
  [:div.py-5
   [:h2.text-center.display-4.pb-5 "Häufig Gestellte Fragen zu schnaq Abos"]
   [:section
    [:h3.text-center.display-5.font-weight-bold.text-dark-blue
     "Kann ich jederzeit kündigen?"]
    [:p.display-6.text-center.pb-3
     [:span.text-primary "Ja! "] "Sie können" [:span.text-primary " jeden Monat"] " kündigen,
     wenn Sie die monatliche Zahlweise gewählt haben. Wenn Sie die jährliche Zahlweise
     wählen, können Sie zum Ablauf des Abonnementjahres kündigen."]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     "Muss ich für mehr Leute extra bezahlen?"]
    [:p.display-6.text-center.pb-3
     [:span.text-primary "Nein, "] "Sie können" [:span.text-primary " beliebig viele Personen "]
     " zu Ihrer Organisation hinzufügen. Jedes Unternehmen, Verein,
     Bildungseinrichtung, usw. braucht " [:span.text-primary "nur ein Abonnement."]]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     "Verlängert sich der Test-Zeitraum automatisch?"]
    [:p.display-6.text-center.pb-3
     [:span.text-primary "Nein, "] "wenn ihr Test-Zeitraum endet können Sie" [:span.text-primary " aktiv Entscheiden "]
     " ob Sie Zahlungsdaten hinzufügen und weiter den Business-Tarif nutzen möchten.
     Der " [:span.text-primary "Starter Plan bleibt unbegrenzt kostenfrei"] " auch nach dem Test-Zeitraum."]]
   [:section
    [:h3.text-center.display-5.font-weight-bold.text-dark-blue
     "Kann ich den Testzeitraum verlängern?"]
    [:p.display-6.text-center.pb-3
     [:span.text-primary "Ja! "] "Schreiben Sie uns einfach eine " [:span.text-primary " E-Mail"] " an "
     [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com."]]]
   [:section
    [:h3.text-center.display-5.pt-3.font-weight-bold.text-dark-blue
     "Wer hat Zugriff auf meine Daten?"]
    [:p.display-6.text-center.pb-3
     "Jede Person die Sie Ihrem Unternehmen hinzufügen kann potentiell auf die hinterlegten Daten zugreifen."
     "Technisch werden Ihre Daten vollständig sicher auf"
     [:span.text-primary " deutschen Servern und DSGVO-Konform"] " abgespeichert. Mehr Informationen
     dazu finden Sie auf unserer " [:a {:href (reititfe/href :routes/privacy)} "Datenschutzsseite."]]]])

(defn- pricing-page
  "A full page depicting our pricing and related items."
  []
  [pages/with-nav-and-header
   {:page/heading [:span.d-block.text-center "Schnaq Abonnement"]}
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