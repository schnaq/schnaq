(ns schnaq.interface.events
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [schnaq.interface.db :as schnaq-db]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.modals.modal :as modal]))

(rf/reg-event-fx
  :username/from-localstorage
  (fn [{:keys [db]} _]
    (if-let [name (ls/get-item :username)]
      {:db (assoc-in db [:user :name] name)}
      {:fx [[:dispatch [:username/notification-set-name]]]})))

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
  :load/meetings
  (fn [_ _]
    (when-not toolbelt/production?
      {:fx [[:http-xhrio {:method :get
                          :uri (str (:rest-backend config) "/meetings")
                          :timeout 10000
                          :response-format (ajax/transit-response-format)
                          :on-success [:init-from-backend]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :load/last-added-meeting
  (fn [_ _]
    (let [share-hash (ls/get-item :meeting.last-added/share-hash)
          edit-hash (ls/get-item :meeting.last-added/edit-hash)]
      (when-not (and (nil? edit-hash) (nil? share-hash))
        {:fx [[:dispatch [:meeting/load-by-hash-as-admin share-hash edit-hash]]]}))))

(rf/reg-event-fx
  :initialise-db
  (fn [_ _]
    {:db schnaq-db/default-db
     :fx [[:dispatch [:load/meetings]]
          [:dispatch [:username/from-localstorage]]
          [:dispatch [:load/last-added-meeting]]
          [:dispatch [:meetings.save-admin-access/store-hashes-from-localstorage]]
          [:dispatch [:agendas.save-statement-nums/store-hashes-from-localstorage]]
          [:dispatch [:meetings.visited/store-hashes-from-localstorage]]]}))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-meetings]]
    (assoc-in db [:meetings :all] all-meetings)))

(rf/reg-event-db
  :admin/set-password
  (fn [db [_ password]]
    (assoc-in db [:admin :password] password)))

(rf/reg-event-fx
  :form/should-clear
  (fn [_ [_ form-elements]]
    {:fx [[:form/clear form-elements]]}))

(rf/reg-sub
  :current-locale
  (fn [db _]
    (get db :locale)))

(rf/reg-event-db
  :set-locale
  (fn [db [_ locale]]
    (assoc db :locale locale)))