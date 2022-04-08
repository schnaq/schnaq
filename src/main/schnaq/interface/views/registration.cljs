(ns schnaq.interface.views.registration
  (:require [ajax.core :as ajax]
            [com.fulcrologic.guardrails.core :refer [=> >defn- ?]]
            [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.http :as http]
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

(defn- next-button [label on-click-fn]
  [buttons/button label on-click-fn "btn-primary w-100 mt-3"])

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

(def toc
  [:p "Ich habe die Datenschutzerklärung gelesen und willige ein, dass meine Daten verarbeitet werden dürfen."])

(defn- registration-step-1
  "First step in the registration process."
  []
  [registration-card
   1
   "Registrieren und schnaqqen"
   [:<>
    [social-logins]
    [:p.text-muted.mb-1.pt-4 "Oder nutze deine Mail:"]
    [:form {:on-submit
            (fn [e] (.preventDefault e)
              (let [form (oget e [:target :elements])
                    email (oget form [:email :value])
                    password (oget form [:password :value])
                    opt-in (oget form [:opt-in :checked])]
                (if (and email password opt-in)
                  (rf/dispatch [:registration/register [email password]])
                  (rf/dispatch [:notification/add
                                #:notification{:title "Bitte alle Felder ausfüllen"
                                               :body "Mindestens eins deiner Felder wurde nicht ausgefüllt. Bitte kontrolliere deine Eingabe."
                                               :context :warning}]))))}
     [inputs/floating "Gib hier deine E-Mail-Adresse ein" :email "registration-email" "email" {:required true :class "mb-2"}]
     [inputs/floating "Bitte vergib nun ein sicheres Passwort" :password "registration-password" "password" {:required true :class "mb-2"}]
     [inputs/checkbox [:small toc] "registration-opt-in" "opt-in" {:required true}]
     [next-button "Kostenfrei registrieren"]]]
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
    [next-button "Weiter"]]])

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

(rf/reg-event-fx
 :registration/register
 (fn [{:keys [db]} [_ [email password]]]
   {:fx [[:dispatch [:registration.go-to/step-2 email]]
         (http/xhrio-request db :post "/user/registration/new"
                             [:registration.register/success]
                             {:email email
                              :password password})]}))

(rf/reg-event-fx
 :registration.register/success
 (fn [{:keys [db]} [_ {:keys [tokens]}]]
   {:fx [[:keycloak.init/with-token [(get-in db [:user :keycloak])
                                     (:access_token tokens)
                                     (:refresh_token tokens)]]]}))

(comment
  (rf/dispatch [:registration/register ["meter+new12@mailbox.org" "123456"]])

  (.log js/console (get-in @re-frame.db/app-db [:user :keycloak]))

  (get-in @re-frame.db/app-db [:user :keycloak])

  nil)
