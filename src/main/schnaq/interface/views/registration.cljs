(ns schnaq.interface.views.registration
  (:require [com.fulcrologic.guardrails.core :refer [>defn- ? =>]]
            [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.views.pages :as pages]))

(defn- social-logins
  "Show social login buttons for direct access."
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

(defn- next-button [on-click-fn]
  [buttons/button "Weiter" nil "btn-primary w-100 mt-3"])

(>defn- registration-card
  "Wrapper to build a common registration card for the complete registration
  process."
  [step heading body footer]
  [number? string? :re-frame/component (? :re-frame/component) => :re-frame/component]
  [:section.col-11.col-md-6.mx-auto.pt-5
   [:div.common-card
    [:div.col-6.col-md-4.mx-auto
     [:a {:href (navigation/href :routes/startpage)}
      [schnaq-logo]]
     [:p.text-center.text-muted (format "Schritt %d von 3" step)]]
    [:section.w-75.mx-auto
     [:h1.h4.text-center.py-3 heading]
     body]]
   footer])

;; -----------------------------------------------------------------------------

(defn- registration-step-1
  "First step in the registration process."
  []
  [registration-card
   1
   "Registrieren und schnaqqen"
   [:<>
    [social-logins]
    [:p.text-muted.mb-1.pt-4 "Oder nutze deine Mail:"]
    [:form {:on-submit (fn [e] (.preventDefault e)
                         (rf/dispatch [:registration.go-to/step-2 (oget e [:target :elements :email :value])]))}
     [inputs/floating "register-email" "Gib hier deine E-Mail-Adresse ein" :email "email" {:required true}]
     [next-button]]]
   [:div.text-center
    [:span "Du hast bereits einen Account?"]
    [buttons/button "Melde dich an" identity "btn-link" {:id "registration-first-step"}]]])

(defn registration-step-1-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title "Registrierung"}
   [registration-step-1]])

;; -----------------------------------------------------------------------------

(defn- checkbox [id label icon-key]
  [:<>
   [:input.btn-check {:id id :type :checkbox :autoComplete :off :name id}]
   [:label.btn.btn-outline-dark.mx-1 {:for id :style {:width "33%"}}
    [icon icon-key "m-1" {:size :lg}]
    [:p.mb-0 label]]])

(defn- survey
  "Ask user where she wants to use schnaq for."
  []
  [:section
   [:div.d-flex.flex-row.pb-2
    [checkbox "education" "Lehre" :graduation-cap]
    [checkbox "coachings" "Coachings" :university]
    [checkbox "seminars" "Seminare" :rocket]]
   [:div.d-flex.flex-row
    [checkbox "fairs" "Messen" :briefcase]
    [checkbox "meetings" "(Online) Meetings" :laptop]
    [checkbox "other" "Anderes" :magic]]])

(defn- registration-step-2
  "First step in the registration process."
  []
  [registration-card
   2
   "Wobei wird schnaq dich unterstützen?"
   [:<>
    [:p.text-muted.mb-1 "Wähle alles passende aus"]
    [survey]
    [next-button]]])

(defn registration-step-2-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title "Registrierung"}
   [registration-step-2]])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :registration.go-to/step-2
 (fn [{:keys [db]} [_ email]]
   {:db (assoc-in db [:registration :email] email)
    :fx [[:dispatch [:navigation/navigate :routes.user.register/step-2]]]}))
