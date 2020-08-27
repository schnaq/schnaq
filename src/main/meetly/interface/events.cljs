(ns meetly.interface.events
  (:require [ajax.core :as ajax]
            [meetly.interface.config :refer [config]]
            [meetly.interface.db :as meetly-db]
            [meetly.interface.utils.localstorage :as ls]
            [meetly.interface.utils.toolbelt :as toolbelt]
            [meetly.interface.views.modals.modal :as modal]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  :load/username
  (fn [{:keys [db]} _]
    (if-let [name (ls/get-item :username)]
      {:db (assoc-in db [:db :user :name] name)}
      {:fx [[:dispatch [:user/set-display-name "Anonymous"]]
            [:dispatch [:modal {:show? true
                                :child [modal/enter-name-modal]}]]]})))

(rf/reg-event-fx
  :load/meetings
  (fn [_ _]
    (when-not toolbelt/production?
      {:fx [[:http-xhrio {:method :get
                          :uri (str (:rest-backend config) "/meetings")
                          :timeout 10000
                          :response-format (ajax/transit-response-format)
                          :on-success [:init-from-backend]
                          :on-failure [:ajax-failure]}]]})))


(rf/reg-event-fx
  :initialise-db
  (fn [_ _]
    {:fx [[:dispatch [:load/meetings]]
          [:dispatch [:load/username]]]}))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-meetings]]
    (assoc db :meetings all-meetings)))

(rf/reg-event-db
  :admin/set-password
  (fn [db [_ password]]
    (assoc-in db [:admin :password] password)))

(rf/reg-event-fx
  :form/should-clear
  (fn [_ [_ form-elements]]
    {:form/clear form-elements}))