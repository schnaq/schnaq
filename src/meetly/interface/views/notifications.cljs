(ns meetly.interface.views.notifications
  (:require [goog.dom :as gdom]))

(defn toast [title body]
  [:div.toast {:role "alert" :aria-live "assertive" :aria-atomic "true"}
   [:div.toast-header
    [:strong.mr-auto title]
    [:small.text-muted "11 mins ago"]
    [:button.ml-2.mb-1.close {:type "button" :data-dismiss "toast" :aria-label "Close"}
     [:span {:aria-hidden "true"} "&times;"]]]
   [:div.toast-body body]])

(comment

  :end)

(defn view
  "Presenting all notifications to the user."
  []
  (let [notifications @(rf/subscribe [:notifications/all])]
    [:div#notifications
     {:aria-live "polite"
      :aria-atomic true
      :style {:position "absolute"
              :top "1rem"
              :left "1rem"}}
     [:> AnimatePresence
      (for [notification notifications]
        [:div {:key (:id notification)}
         [toast notification]])]]))


;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  :notification/add
  (fn [{:keys [db]} [_ notification]]
    (let [notification-id (str (random-uuid))
          notification' (assoc notification :id notification-id)]
      {:db (update db :notifications conj notification')
       :notification/timed-remove notification-id})))

(rf/reg-fx
  :notification/timed-remove
  (fn [notification-id]
    (js/setTimeout #(rf/dispatch [:notification/remove notification-id])
                   display-time)))

(rf/reg-event-db
  :notification/remove
  (fn [db [_ notification-id]]
    (when-let [notifications (:notifications db)]
      (assoc db :notifications
                (remove (fn [{:keys [id]}] (= id notification-id)) notifications)))))

(rf/reg-event-db
  :notifications/reset
  (fn [db [_]]
    (assoc db :notifications [])))

(rf/reg-sub
  :notifications/all
  (fn [db]
    (get db :notifications)))