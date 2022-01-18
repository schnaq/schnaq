(ns schnaq.interface.views.user.subscription
  (:require [com.fulcrologic.guardrails.core :refer [>defn- => ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as util-time]))

(defn- subscription-entry
  "A single entry in the description-list."
  [label field]
  [:<>
   [:dt.col-sm-5 label]
   [:dd.col-sm-7 field]])

(>defn- formatted-subscription-date
  "Make human-readable timestamp from epoch."
  [period locale]
  [(? nat-int?) keyword? => string?]
  (let [date (js/Date. (* 1000 period))]
    (gstring/format "%s, %s"
                    (util-time/format-distance date locale)
                    (util-time/formatted-with-timezone date))))

(defn- cancel-subscription-button []
  (let [on-click #(when (js/confirm "Möchtest du dein Abonnement wirklich zum Ablauf des Bezahlzeitraums kündigen?")
                    (rf/dispatch [:user.subscription/cancel true]))]
    [:<>
     [buttons/button "Abonnement beenden" on-click "btn-outline-dark btn-sm mb-3"]
     [common-components/hint-text "Hier kannst du dein Abonnement zum nächstmöglichen Zeitpunkt beenden. Du hast bist zum Ablauf der Frist noch die Möglichkeit alle Pro-Funktionen zu nutzen. Du kannst jederzeit dein Abo hier wieder aktivieren."]]))

(defn- continue-subscription-button []
  (let [on-click #(when (js/confirm "Möchtest du dein Abo wieder aktivieren?")
                    (rf/dispatch [:user.subscription/cancel false]))]
    [:<>
     [buttons/button "Abonnement aktivieren" on-click "btn-outline-secondary mb-3"]
     [common-components/hint-text "Möchtest du dein Abonnement wieder aktivieren? Schade, dass du es beenden möchtest. Bis zum Ende der Laufzeit hast du noch Zugang zu Pro-Funktionen."]]))


;; -----------------------------------------------------------------------------


(defn- status-pill
  "Display a status pill depending on the subscription status."
  []
  (let [{:keys [status]} @(rf/subscribe [:user/subscription])]
    [:span.badge.badge-pill {:class (case status
                                      :active "badge-success"
                                      "badge-warning")}
     status]))

(defn- cancel-indicator []
  (let [{:keys [cancelled?]} @(rf/subscribe [:user/subscription])]
    (when cancelled?
      [subscription-entry "Gekündigt?"
       [icon :check/circle "text-success"]])))

(defn stripe-management []
  (let [{:keys [status cancelled? period-start period-end type]} @(rf/subscribe [:user/subscription])
        locale @(rf/subscribe [:current-locale])]
    (when status
      [:section
       [:h2 "Abonnementeinstellungen"]
       [:dl.row
        [subscription-entry "Status" [status-pill]]
        [subscription-entry "Typ" type]
        [cancel-indicator]
        [subscription-entry "Abonnement gestartet" (formatted-subscription-date period-start locale)]
        [subscription-entry (if cancelled? "Abonnement endet" "Nächste Abrechnung")
         (formatted-subscription-date period-end locale)]]
       (if cancelled?
         [continue-subscription-button]
         [cancel-subscription-button])])))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :user.subscription/status
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :get "/stripe/subscription/status"
                             [:user.subscription.status/success])]}))
(rf/reg-event-db
 :user.subscription.status/success
 (fn [db [_ subscription-status]]
   (when subscription-status
     (update-in db [:user :subscription] merge subscription-status))))

(rf/reg-event-fx
 :user.subscription/cancel
 (fn [{:keys [db]} [_ cancel?]]
   {:fx [(http/xhrio-request db :post "/stripe/subscription/cancel"
                             [:user.subscription.cancel/success cancel?]
                             {:cancel? cancel?}
                             [:user.subscription.cancel/error])]}))

(rf/reg-event-fx
 :user.subscription.cancel/error
 (fn [_ [_ failure]]
   {:fx [[:dispatch [:ajax.error/to-console failure]]
         [:dispatch [:notification/add
                     #:notification{:title "Problem beim Stornieren"
                                    :body [:<>
                                           "Bei der Kündigung deines Abonnements ist ein Fehler aufgetreten. Bitte kontaktiere uns, damit wir dir schnellstmöglich helfen können" " 🤒"
                                           [buttons/anchor "info@schnaq.com" "mailto:info@schnaq.com" "btn-outline-dark"]]
                                    :context :danger
                                    :stay-visible? true}]]]}))
(rf/reg-event-fx
 :user.subscription.cancel/success
 (fn [_ [_ cancel? subscription-status]]
   {:fx [[:dispatch [:user.subscription/status subscription-status]]
         [:dispatch [:notification/add
                     #:notification{:title (if cancel?
                                             "Abonnement erfolgreich gekündigt"
                                             (str "Abonnement erneut aktiviert" " 🎉"))
                                    :body (str
                                           (if cancel?
                                             "Schade, dass du die Pro-Funktionen von schnaq nicht mehr verwenden möchtest. Bis zum Ablauf der aktuellen Bezahlperiode kannst du dich noch umentscheiden."
                                             "Willkommen zurück! Schön, dass du es dir anders überlegt hast.")
                                           " 🍀")
                                    :context :success
                                    :stay-visible? true}]]]}))
