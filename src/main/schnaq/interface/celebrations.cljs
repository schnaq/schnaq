(ns schnaq.interface.celebrations
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]))

(defn- schnaq-filled-body
  "Celebrate user that she added the first statement to a new schnaq."
  []
  [:<>
   [:p (labels :celebrations.schnaq-filled/lead)]
   [:p (labels :celebrations.schnaq-filled/share-now)]
   [:button.btn.btn-sm.btn-secondary
    {:on-click admin-buttons/open-share-modal}
    (labels :celebrations.schnaq-filled/button)]])

(rf/reg-event-fx
  :celebrate/schnaq-filled
  (fn []
    {:fx [[:dispatch [:celebrate/state :first-post true]]
          [:dispatch [:notification/add
                      #:notification{:title (labels :celebrations.schnaq-filled/title)
                                     :body [schnaq-filled-body]
                                     :context :primary
                                     :stay-visible? true}]]]}))

(rf/reg-event-db
  :celebrate/state
  (fn [db [_ celebration-type celebration-state]]
    (assoc-in db [:celebration celebration-type] celebration-state)))

(rf/reg-sub
  :celebrate/state?
  (fn [db [_ celebration-type]]
    (get-in db [:celebration celebration-type] false)))


;; -----------------------------------------------------------------------------

(defn- first-schnaq-created-body
  "Celebrate non-registered user for her first created schnaq."
  []
  [:<>
   [:p (labels :celebrations.first-schnaq-created/lead)]
   [:button.btn.btn-sm.btn-secondary
    {:on-click #(rf/dispatch [:keycloak/login])}
    (labels :user/login)]])

(rf/reg-event-fx
  :celebrate/first-schnaq-created
  (fn [{:keys [db]}]
    (when-not (get-in db [:user :authenticated?])
      {:fx [[:dispatch [:notification/add
                        #:notification{:title (labels :celebrations.first-schnaq-created/title)
                                       :body [first-schnaq-created-body]
                                       :context :primary
                                       :stay-visible? true}]]]})))
