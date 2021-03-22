(ns schnaq.interface.views.user
  (:require [ajax.core :as ajax]
            [clojure.string :as clj-string]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]))

(defn user-info
  "User info box containing relevant information for discussions."
  [username avatar-size]
  [:div.d-flex.flex-row.align-items-center
   [:div.pr-2.pb-1.text-right.user-name
    [:small username]]
   [common/avatar username avatar-size]])

(rf/reg-event-fx
  :user/set-display-name
  (fn [{:keys [db]} [_ username]]
    ;; only update when string contains
    (when-not (clj-string/blank? username)
      (cond-> {:db (assoc-in db [:user :name] username)
               :fx [[:http-xhrio {:method :post
                                  :uri (str (:rest-backend config) "/author/add")
                                  :params {:nickname username}
                                  :format (ajax/transit-request-format)
                                  :response-format (ajax/transit-response-format)
                                  :on-success [:user/hide-display-name-input username]
                                  :on-failure [:ajax.error/as-notification]}]]}
              (not= "Anonymous" username) (update :fx conj [:localstorage/assoc [:username username]])))))

(rf/reg-sub
  :user/display-name
  (fn [db]
    (get-in db [:user :name] "Anonymous")))

(rf/reg-sub
  :user/show-display-name-input?
  (fn [db]
    (get-in db [:controls :username-input :show?] true)))

(rf/reg-event-fx
  :user/hide-display-name-input
  (fn [{:keys [db]} [_ username]]
    (let [notification
          [[:dispatch [:notification/add
                       #:notification{:title (labels :user.button/set-name)
                                      :body (labels :user.button/success-body)
                                      :context :success}]]
           [:dispatch [:notification/remove "username/notification-set-name"]]]]
      ;; Show notification if user is not named "Anonymous"
      (cond-> {:db (assoc-in db [:controls :username-input :show?] false)}
              (not= "Anonymous" username) (assoc :fx notification)))))

(rf/reg-event-db
  :user/show-display-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] true)))