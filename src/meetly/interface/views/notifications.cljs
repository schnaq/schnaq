(ns meetly.interface.views.notifications
  (:require [meetly.interface.text.display-data :refer [fa]]
            [reagent.dom]
            [re-frame.core :as rf]
            ["framer-motion" :refer [motion AnimatePresence]]))

(def ^:private display-time "Milliseconds, that a notification stays visible."
  20000)

(defn toast [{:keys [title body id]}]
  [:> (.-div motion)
   {:initial {:opacity 0}
    :animate {:opacity 1}
    :exit {:opacity 0}}
   [:div.toast.show
    {:aria-atomic "true", :aria-live "assertive", :role "alert"
     :data-autohide false
     :style {:width "15rem"
             :margin-bottom ".5rem"}}
    [:div.toast-header
     [:strong.mr-auto title]
     #_[:small.text-muted "just now"]
     [:button.close {:type "button"
                     :on-click #(rf/dispatch [:notification/remove id])}
      [:span {:aria-hidden "true"}
       [:i {:class (str " m-auto fas fa-xs " (fa :delete-icon))}]]]]
    [:div.toast-body body]]])

(comment
  (.toast (js/$ ".toast") #js {:delay 3000
                               :autohide false})
  (.toast (js/$ ".toast") "show")
  (.toast (js/$ ".toast") "hide")

  (rf/dispatch [:notification/add {:title "Har har!" :body "Ich bin es, der Teufel!"}])
  (rf/dispatch [:notification/add {:title "some" :body "other"}])
  (rf/dispatch [:notifications/reset])

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