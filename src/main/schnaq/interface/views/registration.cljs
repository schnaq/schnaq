(ns schnaq.interface.views.registration
  (:require [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.views.pages :as pages]))

(defn- social-logins
  "TODO"
  []
  (let [styles {:style {:width "50%"}}]
    [:section
     [:p.text-muted.mb-1 "Fortsetzen mit:"]
     [:div.d-flex.flex-row.pb-2
      [buttons/button "Xign.me" identity "btn-outline-dark me-2" styles]
      [buttons/button "Google" identity "btn-outline-dark" styles]]
     [:div.d-flex.flex-row
      [buttons/button "LinkedIn" identity "btn-outline-dark me-2" styles]
      [buttons/button "GitHub" identity "btn-outline-dark" styles]]]))

(defn- start-registration []
  [:div.w-50.mx-auto.pt-5
   [:div.common-card
    [:div.w-25.mx-auto
     [:a {:href (navigation/href :routes/startpage)}
      [schnaq-logo]]]
    [:section.w-75.mx-auto
     [:p.text-center.text-muted "Schritt 1 von 3"]
     [:h1.h4.text-center.py-3 "Registrieren und schnaqqen"]
     [social-logins]
     [inputs/input-floating "register-email" "label" :text "email"]]]])

(defn start-registration-view
  "TODO"
  []
  [pages/fullscreen
   {:pages/title "Registrierung"}
   [start-registration]])
