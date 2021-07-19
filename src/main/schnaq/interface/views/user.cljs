(ns schnaq.interface.views.user
  (:require [clojure.string :as clj-string]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.views.common :as common]
            [schnaq.user :as user-utils]))

(defn user-info
  "User info box displaying user's nickname, timestamp and the avatar."
  [user avatar-size time]
  (let [locale @(rf/subscribe [:current-locale])
        authenticated? (:user.registered/keycloak-id user)
        display-name (user-utils/display-name user)
        name-class (if authenticated? "text-primary" "text-muted")]
    [:div.d-flex.flex-row.text-muted
     [common/avatar user avatar-size]
     [:small.mx-2.my-auto {:class name-class} display-name]
     (when time
       [:small.font-weight-light.d-inline.my-auto
        [time/timestamp-with-tooltip time locale]])]))

(defn user-info-left-to-right
  "User info box displaying user's nickname, timestamp and the avatar from left to right."
  [user avatar-size time]
  (let [locale @(rf/subscribe [:current-locale])
        authenticated? (:user.registered/keycloak-id user)
        display-name (user-utils/display-name user)
        name-class (if authenticated? "text-primary" "text-muted")]
    [:div.d-flex.flex-row.text-muted
     (when time
       [:small.font-weight-light.d-inline.my-auto.ml-2
        [time/timestamp-with-tooltip time locale]])
     [:small.mx-1.my-auto (labels :discussion.badges/statement-by)]
     [:small.mr-2.my-auto {:class name-class} display-name]
     [common/avatar user avatar-size]]))

(rf/reg-event-fx
  :user/set-display-name
  (fn [{:keys [db]} [_ username]]
    ;; only update when string contains
    (when-not (clj-string/blank? username)
      (cond-> {:db (assoc-in db [:user :names :display] username)
               :fx [(http/xhrio-request db :put "/user/add/anonymous" [:user/hide-display-name-input username]
                                        {:nickname username}
                                        [:ajax.error/as-notification])]}
              (not= default-anonymous-display-name username)
              (update :fx conj [:localstorage/assoc [:username username]])))))

(rf/reg-sub
  :user/display-name
  (fn [db _]
    (get-in db [:user :names :display] default-anonymous-display-name)))

(rf/reg-sub
  :user/groups
  (fn [db _]
    (get-in db [:user :groups] [])))

(rf/reg-sub
  :user/show-display-name-input?
  (fn [db]
    (get-in db [:controls :username-input :show?] false)))

(rf/reg-event-fx
  :user/hide-display-name-input
  (fn [{:keys [db]} [_ username]]
    (let [notification
          [[:dispatch [:notification/add
                       #:notification{:title (labels :user.button/set-name)
                                      :body (labels :user.button/success-body)
                                      :context :success}]]]]
      ;; Show notification if user is not default anonymous display name
      (cond-> {:db (assoc-in db [:controls :username-input :show?] false)}
              (not= default-anonymous-display-name username) (assoc :fx notification)))))

(rf/reg-event-db
  :user/show-display-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] true)))

(rf/reg-sub
  :user/current
  (fn [db] (:user db)))