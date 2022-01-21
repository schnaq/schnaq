(ns schnaq.interface.views.user.subscription
  (:require [com.fulcrologic.guardrails.core :refer [>defn- => ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
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
  (let [on-click #(when (js/confirm (labels :subscription.cancel/confirmation))
                    (rf/dispatch [:user.subscription/cancel true]))]
    [:<>
     [buttons/button (labels :subscription.cancel/button) on-click "btn-outline-dark btn-sm mb-3"]
     [common-components/hint-text (labels :subscription.cancel/button-hint)]]))

(defn- continue-subscription-button []
  (let [on-click #(when (js/confirm (labels :subscription.reactivate/confirmation))
                    (rf/dispatch [:user.subscription/cancel false]))]
    [:<>
     [buttons/button (labels :subscription.reactivate/button) on-click "btn-outline-secondary mb-3"]
     [common-components/hint-text (labels :subscription.reactivate/button-hint)]]))


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
      [subscription-entry (labels :subscription.overview/cancelled?)
       [icon :check/circle "text-success"]])))

(defn stripe-management []
  (let [{:keys [status cancelled? period-start period-end type]} @(rf/subscribe [:user/subscription])
        locale @(rf/subscribe [:current-locale])]
    (when status
      [:section
       [:h2 "Abonnementeinstellungen"]
       [:dl.row
        [subscription-entry (labels :subscription.overview/status) [status-pill]]
        [subscription-entry (labels :subscription.overview/type) type]
        [cancel-indicator]
        [subscription-entry (labels :subscription.overview/started-at) (formatted-subscription-date period-start locale)]
        [subscription-entry (if cancelled? (labels :subscription.overview/stops-at) (labels :subscription.overview/next-invoice))
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
                     #:notification{:title (labels :subscription.cancel.error/title)
                                    :body [:<>
                                           [:p (labels :subscription.cancel.error/body) " 🤒"]
                                           [buttons/anchor "info@schnaq.com" "mailto:info@schnaq.com" "btn-outline-dark"]]
                                    :context :danger
                                    :stay-visible? true}]]]}))
(rf/reg-event-fx
 :user.subscription.cancel/success
 (fn [_ [_ cancel? subscription-status]]
   {:fx [[:dispatch [:user.subscription/status subscription-status]]
         [:dispatch [:notification/add
                     #:notification{:title (if cancel?
                                             (labels :subscription.cancel.success/title)
                                             (str (labels :subscription.reactivated.success/title) " 🎉"))
                                    :body (str
                                           (if cancel?
                                             (labels :subscription.cancel.success/body)
                                             (labels :subscription.reactivated.success/body))
                                           " 🍀")
                                    :context :success
                                    :stay-visible? true}]]]}))
