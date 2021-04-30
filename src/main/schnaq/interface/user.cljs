(ns schnaq.interface.user
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.modals.modal :as modal]))

(rf/reg-event-fx
  :username/from-localstorage
  (fn [{:keys [db]} _]
    ;; DEPRECATED, deleted after 2021-09-22: Remove old-name and only use name from first or clause
    (let [old-name (ls/get-item :username)
          username (or (:username local-storage) old-name)]
      (if username
        {:db (assoc-in db [:user :names :display] username)}
        {:fx [[:dispatch [:username/notification-set-name]]]}))))

(rf/reg-event-fx
  :username/notification-set-name
  (fn [_ _]
    (let [notification-id "username/notification-set-name"]
      {:fx [[:dispatch
             [:notification/add
              #:notification{:id notification-id
                             :title (labels :user.set-name/dialog-header)
                             :body [:<>
                                    [:p (labels :user.set-name/dialog-lead)]
                                    [:p (labels :user.set-name/dialog-body)]
                                    [:div.mt-2.btn.btn-sm.btn-outline-primary
                                     {:on-click #(rf/dispatch [:username/open-dialog])}
                                     (labels :user.set-name/dialog-button)]]
                             :context :info
                             :stay-visible? true}]]]})))

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
      {:fx [(http/xhrio-request db :put "/user/register" [:user.register/success]
                                {:creation-secrets (get-in db [:discussion :statements :creation-secrets])})]})))

(rf/reg-event-db
  :user.register/success
  ;; todo clear the creation secrets here
  (fn [db [_ {:keys [registered-user]}]]
    (let [{:user.registered/keys [display-name first-name last-name email profile-picture]} registered-user]
      (-> db
          (assoc-in [:user :names :display] display-name)
          (assoc-in [:user :email] email)
          (assoc-in [:user :id] (:db/id registered-user))
          (assoc-in [:user :profile-picture :display] profile-picture)
          (cond-> first-name (assoc-in [:user :names :first] first-name))
          (cond-> last-name (assoc-in [:user :names :last] last-name))))))

(rf/reg-sub
  :user/id
  (fn [db _]
    (get-in db [:user :id])))