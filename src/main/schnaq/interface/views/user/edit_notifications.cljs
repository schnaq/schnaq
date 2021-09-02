(ns schnaq.interface.views.user.edit-notifications
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]))

(defn set-all-to-read []
  [:div.py-5
   [:div.text-center.mt-5.mb-3
    [:button.btn.btn-secondary
     {:on-click (fn [_]
                  (rf/dispatch [:user.notification/mark-all-as-read]))}
     (labels :user.notifications.set-all-to-read/button)]]
   [:small.text-muted (labels :user.notifications.set-all-to-read/info)]])

(defn- interval-dropdown-item [interval]
  [:div.dropdown-item
   {:on-click (fn [_] (rf/dispatch [:user.notification/mail-interval! interval]))}
   (labels interval)])

(defn change-interval-drop-down []
  (let [dropdown-id "dropdownMailInterval"
        current-interval @(rf/subscribe [:user.notification/mail-interval])
        _ (println "Interval: " current-interval)
        daily :notification-mail-interval/daily
        weekly :notification-mail-interval/weekly
        never :notification-mail-interval/never
        interval-display (case current-interval
                           :notification-mail-interval/daily (labels daily)
                           :notification-mail-interval/weekly (labels weekly)
                           :notification-mail-interval/never (labels never)
                           (labels :notification-mail-interval/daily))
        _ (println interval-display)]
    [:div.dropdown
     [:button.btn.btn-outline-dark.dropdown-toggle
      {:id dropdown-id :type "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      interval-display]
     [:div.dropdown-menu {:aria-labelledby dropdown-id}
      [interval-dropdown-item daily]
      [interval-dropdown-item weekly]
      [:div.dropdown-divider]
      [interval-dropdown-item never]]]))

(defn change-update-mail-interval []
  [:<>
   [:div.row.mt-5.mb-3
    [:div.col
     [:h5.text-muted (labels :user.notifications/mails)]]
    [:div.col.text-right
     [change-interval-drop-down]]]
   [:small.text-muted (labels :user.notifications/info)]])

(defn- content []
  [pages/settings-panel
   (labels :user.notifications/header)
   [:<>
    [change-update-mail-interval]
    [set-all-to-read]]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])

(rf/reg-sub
  :user.notification/mail-interval
  (fn [db _]
    (get-in db [:user :notification-mail-interval] :notification-mail-interval/daily)))

(rf/reg-event-fx
  :user.notification/mail-interval!
  (fn [{:keys [db]} [_ interval]]
    {:fx [(http/xhrio-request db :put "/user/notification-mail-interval"
                              [:user.notification/mail-interval-success]
                              {:notification-mail-interval interval}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-fx
  :user.notification/mail-interval-success
  (fn [{:keys [db]} [_ {:keys [updated-user]}]]
    (let [interval (:user.registered/notification-mail-interval updated-user)]
      {:db (assoc-in db [:user :notification-mail-interval] interval)})))

(rf/reg-event-fx
  :user.notification/mark-all-as-read
  (fn [{:keys [db]} [_]]
    {:fx [(http/xhrio-request db :put "/user/mark-all-as-read"
                              [:no-op]
                              {}
                              [:ajax.error/as-notification])]}))
