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
                (if-not (and email password opt-in)
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
 :user.by-email/exists
 (fn [{:keys [db]} [_ email]]
   {:fx [(http/xhrio-request db :get "/user/by-email"
                             [:user.by-email.exists/success]
                             {:email email})]}))

(rf/reg-event-fx
 :user.by-email.exists/success
 (fn [{:keys [db]} [_ {:keys [exists?]}]]
   (if exists?
     {}
     {})))

(rf/reg-event-fx
 :test/register
 (fn [{:keys [db]} [_ email]]
   {:fx [[:http-xhrio {:method :post
                       :uri "https://auth.schnaq.com/auth/realms/development/protocol/openid-connect/token"
                       :format (ajax/json-request-format)
                       :params {:grant_type "password"
                                :client_id "development"
                                :username "meter+new@mailbox.org"
                                :password "123456"}
                       #_#_:headers headers
                       :response-format (ajax/json-response-format)
                       :on-success [:test.register/success]
                       :on-failure [:test.register/success]}]]}))

(rf/reg-event-fx
 :test.register/success
 (fn [{:keys [db]} [_ response]]
   (prn response)
   {}))

(comment
  (rf/dispatch [:test/register])

  (let [kc (get-in @re-frame.db/app-db [:user :keycloak])]
    (-> kc
        (.init #js {:token "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJvOVVZRWlWZlV6cENnTTdRWjlLeXBSOGpfTVAtM21wWG1OU0pGc0YtVmFBIn0.eyJleHAiOjE2NDkyNTUwODksImlhdCI6MTY0OTI1NDc4OSwianRpIjoiZWNmMWU1ZmQtMDkzNy00YmZhLTliODItMGE0YjdhNDY0YTA2IiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNjaG5hcS5jb20vYXV0aC9yZWFsbXMvZGV2ZWxvcG1lbnQiLCJhdWQiOlsiZGV2ZWxvcG1lbnQtYmFja2VuZCIsImFjY291bnQiXSwic3ViIjoiNzJiMmExZjMtZmRlMi00OTBmLWEzMGYtOTQ3MWY5Nzg4OTUxIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZGV2ZWxvcG1lbnQiLCJzZXNzaW9uX3N0YXRlIjoiNDlmNTlmYTQtNThjMS00MGFlLWJiY2QtNDExYTAyYWI2YzIxIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiLCJodHRwczovL2NoZWNrb3V0LnN0cmlwZS5jb20iLCJodHRwOi8vbG9jYWxob3N0OjMwMDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiZGVmYXVsdC1yb2xlcy1kZXZlbG9wbWVudCIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiZGV2ZWxvcG1lbnQtYmFja2VuZCI6eyJyb2xlcyI6WyJhZG1pbiJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJlbWFpbCBwcm9maWxlIiwic2lkIjoiNDlmNTlmYTQtNThjMS00MGFlLWJiY2QtNDExYTAyYWI2YzIxIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImdyb3VwcyI6W10sInByZWZlcnJlZF91c2VybmFtZSI6Im1ldGVyK25ld0BtYWlsYm94Lm9yZyIsImVtYWlsIjoibWV0ZXIrbmV3QG1haWxib3gub3JnIn0.WtUiK2-Vr9ikwKs_4S336Wr2t33PViOeU6BWL1xTwHsK8aqA6W0j2oYBwiVvimi5Gbq6x82KGgShjrH9L0y9NydPbiro5pIKlCGGXzTr57EQiLlkOXbpsN9M7RrGbY-VeZvwy3jeJYEgRzq18oTeGytKTcZ2JkgvY3oURGkYBqzMKOVCud8LWOZjka1dL4uNI26J0SrY5tMT4UyqYsCnK1q9giHOcpE9ZWoBVJBHzvGXC4PdeukZgpoe7IyD8mTXh9Ki6WpM6Hogo0XOBHIwitRFBmj9BPZ3ctOfxyFYalNf9LfgWYVP6Re5--MGapCCca9EFtcijWcHJLl1-AVwcw"
                    :refreshToken "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJkNTg5NjQwNC1mM2QzLTQ5Y2UtYmZhYy01Zjg4MTI5MzNlNDUifQ.eyJleHAiOjE2NDkyNTY1ODksImlhdCI6MTY0OTI1NDc4OSwianRpIjoiYjMwN2RjODEtYjViZS00ZWI0LWE5YzktMmM5ZmExZDA1OGU3IiwiaXNzIjoiaHR0cHM6Ly9hdXRoLnNjaG5hcS5jb20vYXV0aC9yZWFsbXMvZGV2ZWxvcG1lbnQiLCJhdWQiOiJodHRwczovL2F1dGguc2NobmFxLmNvbS9hdXRoL3JlYWxtcy9kZXZlbG9wbWVudCIsInN1YiI6IjcyYjJhMWYzLWZkZTItNDkwZi1hMzBmLTk0NzFmOTc4ODk1MSIsInR5cCI6IlJlZnJlc2giLCJhenAiOiJkZXZlbG9wbWVudCIsInNlc3Npb25fc3RhdGUiOiI0OWY1OWZhNC01OGMxLTQwYWUtYmJjZC00MTFhMDJhYjZjMjEiLCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJzaWQiOiI0OWY1OWZhNC01OGMxLTQwYWUtYmJjZC00MTFhMDJhYjZjMjEifQ.XqtTs8k7s4pdsVHwf6g1iovdBhAHbhpkyJqc320iJP0"})
        (.then (fn [result]
                 (rf/dispatch [:auth/after-successful-login])
                 (rf/dispatch [:user/authenticated! result])
                 (rf/dispatch [:keycloak/load-user-profile])
                 (rf/dispatch [:keycloak.roles/extract])
                 (rf/dispatch [:keycloak/check-token-validity])
                 (rf/dispatch [:user/register result])
                 (rf/dispatch [:hubs.personal/load])))
        (.catch (fn [_]
                  (rf/dispatch [:user/authenticated! false])))))

  (.log js/console (get-in @re-frame.db/app-db [:user :keycloak]))

  (rf/dispatch [:user.by-email/exists "cmeter@googlemail.com"])

  (get-in @re-frame.db/app-db [:user :keycloak])

  nil)
