(ns schnaq.interface.views.subscription
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon-card]]
            [schnaq.interface.views.pages :as pages]))

(defn- next-step [icon title body button-text route-name]
  (let [href (rfe/href route-name)]
    [:article.pb-3.pr-3
     [:a {:href href} [icon-card icon "text-typography" {:size :lg}]]
     [:p.font-weight-bold.my-2 title]
     [:p body]
     [buttons/anchor button-text href :btn-white]]))

(defn- success
  "Celebrating and welcoming the new pro user."
  []
  [pages/with-nav-and-header
   {:page/heading "Du bist startklar 🎉"
    :page/subheading "Von nun an stehen dir alle Pro-Features zur Verfügung."
    :page/vertical-header? true
    :page/classes "base-wrapper bg-typography"
    :page/more-for-heading
    [:section.container {:style {:min-height "50vh"}}
     [:div.pt-5.mt-md-5
      [:div.d-flex.flex-md-row.flex-column
       [next-step :rocket
        "Lege los!"
        "Du kannst nun das volle Potenzial aus deinen schnaqs schöpfen. Dir stehen nun Analysen, Aktivierungsoptionen, Wortwolken und vieles mehr zur Verfügung."
        "Zu deinen schnaqs"
        :routes.schnaqs/personal]
       [next-step :sliders-h
        "Abonnement verwalten"
        "In deinen Einstellungen kannst du jederzeit das Abonnement verwalten. Solltest du Probleme oder Fragen haben, so kontaktiere uns gerne!"
        "Zu den Einstellungen"
        :routes.user.manage/account]]]]}])

(defn- cancel
  "TODO"
  []
  [pages/with-nav-and-header
   {:page/heading "Vorgang abgebrochen"}
   [:section.container
    [:h3.pb-3 "Schade, dass du den Vorgang nicht abgeschlossen hast"]
    [:p.lead "Dir entgeht damit die Möglichkeit das volle Potenzial aus den Interaktionen mit deinen Teilnehmer:innen auszuschöpfen."]
    [:p "Im kostenfreien Plan stehen dir weiterhin alle Basisfunktionen zur Verfügung. Wir würden uns sehr freuen von dir zu hören, warum du doch nicht die Pro-Funktionen verwenden möchtest. Kontaktiere uns dazu gerne 👍 Fehlt dir vielleicht eine Funktion? Lass es uns wissen – wir finden eine Lösung!"]]])

;; -----------------------------------------------------------------------------

(defn success-view
  "Wrapping the success page."
  []
  [success])

(defn cancel-view
  "Wrapping the cancel page."
  []
  [cancel])