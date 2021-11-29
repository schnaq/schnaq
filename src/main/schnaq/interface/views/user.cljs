(ns schnaq.interface.views.user
  (:require [clojure.string :as clj-string]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.time :as util-time]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.common :as common]
            [schnaq.user :as user-utils]))

(defn user-info
  "User info box displaying user's nickname, timestamp and the avatar."
  [statement avatar-size additional-classes]
  (let [locale @(rf/subscribe [:current-locale])
        user (:statement/author statement)
        created (:statement/created-at statement)
        display-name (tools/truncate-to-n-chars (user-utils/display-name user) 15)]
    [:div.d-flex {:class additional-classes}
     [common/avatar user avatar-size]
     [:div.mx-2 [:span.text-sm.text-typography display-name]
      (when created
        [:div.small.font-weight-light.text-muted
         [util-time/timestamp-with-tooltip created locale]])]]))

(defn user-info-only
  "User info box displaying user's nickname and the avatar."
  [user avatar-size]
  (let [authenticated? (:user.registered/keycloak-id user)
        display-name (user-utils/display-name user)
        name-class (if authenticated? "text-typography" "text-muted")]
    [:div.d-flex.flex-row.text-muted
     [:div.d-md-none
      [common/avatar user (* avatar-size 0.75)]]
     [:div.d-none.d-md-block
      [common/avatar user avatar-size]]
     [:small.mx-2.my-auto {:class name-class} display-name]]))

(rf/reg-event-fx
 :user/set-display-name
 (fn [{:keys [db]} [_ username]]
   ;; only update when string contains
   (when-not (clj-string/blank? username)
     (cond-> {:db (assoc-in db [:user :names :display] username)
              :fx [(http/xhrio-request db :put "/user/anonymous/add" [:user/hide-display-name-input username]
                                       {:nickname username}
                                       [:ajax.error/as-notification])]}
       (not= default-anonymous-display-name username)
       (update :fx conj [:localstorage/assoc [:username username]])))))

(rf/reg-sub
 :user/display-name
 (fn [db _]
   (tools/current-display-name db)))

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
