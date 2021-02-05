(ns schnaq.interface.views.meeting.meetings
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]))

;; #### Events ####

(rf/reg-event-fx
  :schnaq/select-current
  (fn [{:keys [db]} [_ discussion]]
    {:db (assoc-in db [:schnaq :selected] discussion)
     :fx [[:dispatch [:schnaq.visited/to-localstorage (:discussion/share-hash discussion)]]]}))

(rf/reg-sub
  :schnaq/selected
  (fn [db _]
    (get-in db [:schnaq :selected])))

(rf/reg-event-fx
  :schnaq/load-by-share-hash
  (fn [_ [_ hash]]
    {:fx [[:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/schnaq/by-hash/" hash)
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:schnaq/select-current]
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  :meeting/check-admin-credentials
  (fn [_ [_ share-hash edit-hash]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/credentials/validate")
                        :params {:share-hash share-hash
                                 :edit-hash edit-hash}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:meeting/check-admin-credentials-success]
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
  ;; the backend every time)
  :meeting/check-admin-credentials-success
  (fn [_ [_ {:keys [valid-credentials?]}]]
    (when-not valid-credentials?
      {:fx [[:dispatch [:navigation/navigate :routes/forbidden-page]]]})))

(rf/reg-event-db
  :schnaq/save-as-last-added
  (fn [db [_ {:keys [discussion]}]]
    (assoc-in db [:schnaq :last-added] discussion)))

(rf/reg-sub
  :schnaq/last-added
  (fn [db _]
    (get-in db [:schnaq :last-added])))

(rf/reg-event-fx
  :schnaq/error-remove-hashes
  (fn [_ [_ response]]
    {:fx [[:dispatch [:ajax.error/as-notification response]]
          [:localstorage/remove :schnaq.last-added/edit-hash]
          [:localstorage/remove :schnaq.last-added/share-hash]]}))

(rf/reg-event-fx
  :schnaq/load-by-hash-as-admin
  (fn [_ [_ share-hash edit-hash]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/schnaq/by-hash-as-admin")
                        :params {:share-hash share-hash
                                 :edit-hash edit-hash}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:schnaq/save-as-last-added]
                        :on-failure [:schnaq/error-remove-hashes]}]]}))
