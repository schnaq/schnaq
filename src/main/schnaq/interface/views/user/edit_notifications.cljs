(ns schnaq.interface.views.user.edit-notifications
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]))

(defn- check-all-read
  "Displays a check icon when the server responded to mark-all-as-read request"
  []
  (when @(rf/subscribe [:user.notification/mark-all-as-read-finished?])
    [:div.flex.mx-3.my-auto
     [motion/fade-in-and-out
      [motion/move-in :bottom
       [fa :check/normal "text-secondary m-auto"]]]]))

(defn- button-or-spinner
  "Show button per default or spinner while waiting or a server response"
  []
  (if @(rf/subscribe [:user.notification/mark-all-as-read-in-progress?])
    [:div.spinner-border.text-secondary {:role "status"}]
    [:button.btn.btn-outline-secondary
     {:on-click (fn [_] (rf/dispatch [:user.notification/mark-all-as-read!]))}
     (labels :user.notifications.set-all-to-read/button)]))

(defn- set-all-to-read
  "Display button and text for mark-all-as-read related content"
  []
  [:div.py-5
   [:div.mt-5.mb-3
    [:div.d-flex.flex-row.justify-content-center
     [button-or-spinner]
     [check-all-read]]]
   [:small.text-muted (labels :user.notifications.set-all-to-read/info)]])

(defn- interval-dropdown-item
  "Dropdown item for interval options"
  [interval]
  [:div.dropdown-item
   {:on-click (fn [_] (rf/dispatch [:user.notification/mail-interval! interval]))}
   (labels interval)])

(defn- change-interval-drop-down
  "Dropdown to configure mail interval"
  []
  (let [dropdown-id "dropdownMailInterval"
        current-interval @(rf/subscribe [:user.notification/mail-interval])
        daily :notification-mail-interval/daily
        weekly :notification-mail-interval/weekly
        never :notification-mail-interval/never
        interval-display (case current-interval
                           :notification-mail-interval/daily (labels daily)
                           :notification-mail-interval/weekly (labels weekly)
                           :notification-mail-interval/never (labels never)
                           (labels :notification-mail-interval/daily))]
    [:div.dropdown.mx-3
     [:button.btn.btn-outline-dark.dropdown-toggle
      {:id dropdown-id :type "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      interval-display]
     [:div.dropdown-menu {:aria-labelledby dropdown-id}
      [interval-dropdown-item daily]
      [interval-dropdown-item weekly]
      [:div.dropdown-divider]
      [interval-dropdown-item never]]]))

(defn- change-update-mail-interval
  "Display change-mail-interval related content"
  []
  [:<>
   [:div.row.mt-5.mb-3.pt-5
    [:div.col
     [:h5.text-muted (labels :user.notifications/mails)]]
    [:div.col.text-right
     [change-interval-drop-down]]]
   [:small.text-muted (labels :user.notifications/info)]])

(defn- content
  "Display mail interval and mark as read content"
  []
  [pages/settings-panel
   (labels :user.notifications/header)
   [:<>
    [change-update-mail-interval]
    [set-all-to-read]]])

(defn view []
  [settings/user-view
   :user/edit-account
   [content]])

;; subs

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
  :user.notification/mark-all-as-read!
  (fn [{:keys [db]} [_]]
    {:db (assoc-in db [:user :settings :temporary :mark-all-as-read-in-progress?] true)
     :fx [(http/xhrio-request db :put "/user/mark-all-as-read"
                              [:user.notification/mark-all-as-read-success]
                              {}
                              [:ajax.error/as-notification])]}))

(rf/reg-sub
  :user.notification/mark-all-as-read-in-progress?
  (fn [db _]
    (get-in db [:user :settings :temporary :mark-all-as-read-in-progress?] false)))

(rf/reg-sub
  :user.notification/mark-all-as-read-finished?
  (fn [db _]
    (get-in db [:user :settings :temporary :mark-all-as-finished?] false)))


(rf/reg-event-db
  :user.notification/mark-all-as-read-success
  (fn [db _]
    (-> db
        (assoc-in [:user :settings :temporary :mark-all-as-finished?] true)
        (assoc-in [:user :settings :temporary :mark-all-as-read-in-progress?] false))))
