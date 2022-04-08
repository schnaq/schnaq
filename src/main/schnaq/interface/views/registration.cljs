(ns schnaq.interface.views.registration
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn- ?]]
            [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.config :as config]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.pricing :as pricing-view]))

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
  [heading body footer {:keys [step class]}]
  [string? :re-frame/component (? :re-frame/component) map? => :re-frame/component]
  [:section.mx-auto.pt-5 {:class (or class "col-11 col-md-6")}
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
     [inputs/floating "Bitte vergib nun ein sicheres Passwort" :password "registration-password" "password" {:required true :class "mb-2" :minLength 8}]
     [inputs/checkbox [:small toc] "registration-opt-in" "opt-in" {:required true}]
     [next-button "Kostenfrei registrieren"]]]
   [:div.text-center
    [:span "Du hast bereits einen Account?"]
    [buttons/button "Melde dich an" identity "btn-link" {:id "registration-first-step"}]]
   {:step 1}])

(defn registration-step-1-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title "Willkommen bei der Accounterstellung"}
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
   "Wobei wird schnaq dich unterstützen?"
   [:<>
    [:p.text-muted.mb-1 "Wähle alles passende aus"]
    [:form
     {:on-submit
      (fn [e] (.preventDefault e)
        (let [form (oget e [:target :elements])]
          (rf/dispatch [:registration/store-survey-selection form])))}
     [survey]
     [next-button "Weiter"]]]
   nil
   {:step 2}])

(defn registration-step-2-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title "Willkommen bei der Accounterstellung"}
   [registration-step-2]])

;; -----------------------------------------------------------------------------

(defn- list-item
  "Style a list item in the feature list."
  [content]
  [:li.list-group-item.border-0.p-0
   [:span.fa-li [icon :check/normal "text-primary me-2" {:size :xs}]]
   content])

(>defn- tier-card
  "Small tier cards, as a preview on the features."
  [title subtitle price button features additional-classes]
  [string? string? :re-frame/component :re-frame/component :re-frame/component (? string?) => :re-frame/component]
  [:article.col-12.col-md-4.px-1.pb-2
   [:div.card.shadow-sm {:class additional-classes}
    [:div.card-body
     [:div {:style {:min-height "12rem"}}
      [:p.h5.card-title title]
      [:p.h6.card-subtitle.mb-3.text-muted subtitle]
      [:div.text-center.py-3 price]]
     [:div.text-center button]
     [:hr]
     [:small features]]]])

(defn- pro-tier-cta-button
  "Button to open the checkout page."
  []
  (let [yearly? @(rf/subscribe [:pricing.interval/yearly?])
        price-id (:id @(rf/subscribe [(if yearly? :pricing.pro/yearly :pricing.pro/monthly)]))]
    [buttons/button "Pro abonnieren"
     #(rf/dispatch [:subscription/create-checkout-session price-id])
     "btn-secondary"]))

(defn- free-tier-card
  "Show a free tier card."
  []
  [tier-card
   (labels :pricing.free-tier/title)
   (labels :pricing.free-tier/subtitle)
   [:span.display-6 "0 €"]
   [buttons/anchor "Fortsetzen mit Free" (navigation/href :routes.welcome/free) "btn-primary"]
   [:ul.fa-ul.list-group.list-group-flush
    [list-item (format (labels :pricing.features/number-of-users) config/max-concurrent-users-free-tier)]
    [list-item "Dynamisches Q&A"]
    [list-item "Teilbar per QR Code"]]])

(defn- pro-tier-card
  "Show the pro tier card."
  []
  [tier-card
   (labels :pricing.pro-tier/title)
   (labels :pricing.pro-tier/subtitle)
   [pricing-view/price-tag-pro-tier "display-6"]
   [pro-tier-cta-button]
   [:<>
    [:strong "Alle Free-Features, plus:"]
    [:ul.fa-ul.list-group.list-group-flush
     [list-item (format (labels :pricing.features/number-of-users) config/max-concurrent-users-pro-tier)]
     [list-item "Umfragen"]
     [list-item "Schnellaktivierungen"]
     [list-item "Moderationsoptionen"]
     [list-item "Persönliches Design"]]]
   "border-primary shadow"])

(defn- enterprise-tier-card
  "Show the enterprise tier card."
  []
  [tier-card
   (labels :pricing.enterprise-tier/title)
   (labels :pricing.enterprise-tier/subtitle)
   [:span.display-6 (labels :pricing.enterprise-tier/on-request)]
   [pricing-view/enterprise-cta-button]
   [:<>
    [:strong "Alle Pro-Features, plus:"]
    [:ul.fa-ul.list-group.list-group-flush
     [list-item (labels :pricing.features.number-of-users/unlimited)]
     (for [label (take 3 (rest (labels :pricing.features/enterprise)))]
       (with-meta
         [list-item label]
         {:key (str "list-item-" label)}))]]])

(defn- registration-step-3
  "First step in the registration process."
  []
  [registration-card
   "Wählen deinen Plan"
   [:<>
    [:div.row
     [free-tier-card]
     [pro-tier-card]
     [enterprise-tier-card]]
    [:div.text-center
     [buttons/anchor "Compare plans" (navigation/href :routes/pricing) "btn-link"]]]
   nil
   {:step 3
    :class "col-12 col-md-8"}])

(defn registration-step-3-view
  "Wrapped view for usage in routes."
  []
  [pages/fullscreen
   {:page/title "Willkommen bei der Accounterstellung"}
   [registration-step-3]])

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

(rf/reg-event-fx
 :registration/store-survey-selection
 (fn [{:keys [db]} [_ form]]
   (let [education (oget form [:education :checked])
         coachings (oget form [:coachings :checked])
         seminars (oget form [:seminars :checked])
         fairs (oget form [:fairs :checked])
         meetings (oget form [:meetings :checked])
         other (oget form [:other :checked])
         topics (cond-> []
                  education (conj :surveys.using-schnaq-for.topics/education)
                  coachings (conj :surveys.using-schnaq-for.topics/coachings)
                  seminars (conj :surveys.using-schnaq-for.topics/seminars)
                  fairs (conj :surveys.using-schnaq-for.topics/fairs)
                  meetings (conj :surveys.using-schnaq-for.topics/meetings)
                  other (conj :surveys.using-schnaq-for.topics/other))]
     {:fx [(http/xhrio-request db :post "/surveys/participate/using-schnaq-for"
                               [:registration.store-survey-selection/success]
                               {:topics topics})]})))

(rf/reg-event-fx
 :registration.store-survey-selection/success
 (fn [{:keys [db]}]
   {:fx [[:dispatch [:navigation/navigate :routes.user.register/step-3]]]}))

(comment
  (rf/dispatch [:registration/register ["meter+new13@mailbox.org" "123456"]])

  (.log js/console (get-in @re-frame.db/app-db [:user :keycloak]))

  (get-in @re-frame.db/app-db [:user :keycloak])

  nil)
