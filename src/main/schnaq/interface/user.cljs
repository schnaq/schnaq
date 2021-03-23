(ns schnaq.interface.user
  (:require [ajax.core :as ajax]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.config :refer [config]]
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
        {:db (assoc-in db [:user :name] username)}
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
    (let [username (get-in db [:user :name])]
      (when (or (= "Anonymous" username)
                (nil? username))
        {:fx [[:dispatch [:user/set-display-name "Anonymous"]]
              [:dispatch [:modal {:show? true
                                  :child [modal/enter-name-modal]}]]]}))))

(rf/reg-event-fx
  ;; Registers a user in the backend. Sets the returned user in the db
  :user/register
  (fn [{:keys [db]} [_ result]]
    (when result
      {:fx [[:http-xhrio {:method :put
                          :uri (str (:rest-backend config) "/user/register")
                          :format (ajax/transit-request-format)
                          :headers (auth/authentication-header db)
                          :response-format (ajax/transit-response-format)
                          :on-success [:user.register/success]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-db
  :user.register/success
  (fn [db [_ {:keys [registered-user]}]]
    (let [{:user.registered/keys [display-name first-name last-name email]} registered-user]
      (-> db
          (assoc-in [:user :names :display] display-name)
          (assoc-in [:user :email] email)
          (cond-> first-name (assoc-in [:user :names :first] first-name))
          (cond-> last-name (assoc-in [:user :names :last] last-name))))))