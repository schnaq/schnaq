(ns schnaq.interface.user
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.views.modal :as modal]))

(rf/reg-event-fx
  :username/from-localstorage
  (fn [{:keys [db]} _]
    ;; DEPRECATED, deleted after 2021-09-22: Remove old-name and only use name from first or clause
    (let [old-name (ls/get-item :username)
          username (or (:username local-storage) old-name)]
      (when username
        {:db (assoc-in db [:user :names :display] username)}))))

(rf/reg-event-fx
  :username/open-dialog
  (fn [{:keys [db]} _]
    (let [username (get-in db [:user :names :display])]
      (when (or (= default-anonymous-display-name username)
                (nil? username))
        {:fx [[:dispatch [:user/set-display-name default-anonymous-display-name]]
              [:dispatch [:modal {:show? true
                                  :child [modal/enter-name-modal]}]]]}))))

(rf/reg-event-fx
  ;; Registers a user in the backend. Sets the returned user in the db
  :user/register
  (fn [{:keys [db]} [_ result]]
    (when result
      (let [creation-secrets (get-in db [:discussion :statements :creation-secrets])
            visited-hashes (get-in db [:schnaqs :visited-hashes])
            visited-statements (get-in db [:visited :statement-ids])]
        {:fx [(http/xhrio-request db :put "/user/register" [:user.register/success]
                                  (cond-> {:visited-hashes visited-hashes
                                           :visited-statement-ids visited-statements}
                                          creation-secrets (assoc :creation-secrets creation-secrets)))]}))))

(rf/reg-event-fx
  :user.register/success
  (fn [{:keys [db]} [_ {:keys [registered-user updated-statements?]}]]
    (let [{:user.registered/keys [display-name first-name last-name email profile-picture visited-schnaqs]}
          registered-user
          current-route (get-in db [:current-route :data :name])
          visited-hashes (map :discussion/share-hash visited-schnaqs)]
      {:db (-> db
               (assoc-in [:user :names :display] display-name)
               (assoc-in [:user :email] email)
               (assoc-in [:user :id] (:db/id registered-user))
               (assoc-in [:user :profile-picture :display] profile-picture)
               (cond-> first-name (assoc-in [:user :names :first] first-name))
               (cond-> last-name (assoc-in [:user :names :last] last-name))
               ;; Clear secrets, they have been persisted.
               (assoc-in [:discussion :statements :creation-secrets] {}))
       :fx [[:localstorage/dissoc :discussion/creation-secrets]
            [:dispatch [:schnaqs.visited/merge-registered-users-visits visited-hashes]]
            (when (and updated-statements? (= current-route :routes.schnaq.select/statement))
              ;; The starting-statement view is updated automatically anyway
              [:dispatch [:discussion.query.statement/by-id]])]})))

(rf/reg-sub
  :user/id
  (fn [db _]
    (get-in db [:user :id])))