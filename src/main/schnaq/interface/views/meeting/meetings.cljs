(ns schnaq.interface.views.meeting.meetings
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]))

;; #### Events ####

(rf/reg-event-fx
  :schnaq/select-current
  (fn [{:keys [db]} [_ {:discussion/keys [share-hash edit-hash] :as discussion}]]
    {:db (cond->
           db
           true (assoc-in [:schnaq :selected] discussion)
           edit-hash (update-in [:schnaqs :admin-access]
                                assoc share-hash edit-hash))
     :fx [[:dispatch [:schnaq.visited/to-localstorage share-hash]]]}))

(rf/reg-sub
  :schnaq/selected
  (fn [db _]
    (get-in db [:schnaq :selected])))

(rf/reg-sub
  :schnaq/share-hash
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (:discussion/share-hash selected-schnaq)))

(rf/reg-sub
  :schnaq.selected/read-only?
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (not (nil? (some #{:discussion.state/read-only} (:discussion/states selected-schnaq))))))

(rf/reg-event-fx
  :schnaq/load-by-share-hash
  (fn [{:keys [db]} [_ hash]]
    {:fx [(http/xhrio-request db :get (str "/schnaq/by-hash/" hash) [:schnaq/select-current])]}))

(rf/reg-event-fx
  :schnaq/check-admin-credentials
  (fn [{:keys [db]} [_ share-hash edit-hash]]
    {:fx [(http/xhrio-request db :post "/credentials/validate" [:meeting/check-admin-credentials-success]
                              {:share-hash share-hash
                               :edit-hash edit-hash}
                              [:ajax.error/as-notification])]}))

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
  :schnaq/load-by-hash-as-admin
  (fn [{:keys [db]} [_ share-hash edit-hash]]
    {:fx [(http/xhrio-request db :post "/schnaq/by-hash-as-admin" [:schnaq/save-as-last-added]
                              {:share-hash share-hash
                               :edit-hash edit-hash})]}))
