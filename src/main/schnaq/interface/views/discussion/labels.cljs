(ns schnaq.interface.views.discussion.labels
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.shared-toolbelt :as shared-tools]))

(defn build-label
  "Takes a label and builds the necessary html."
  [label set? hover?]
  (let [[badge-color icon-name]
        (case label
          ":comment" ["label-blue" :comment]
          ":arrow-right" ["label-purple" :arrow-right]
          ":calendar-alt" ["label-yellow" :calendar-alt]
          ":check" ["label-green" :check/normal]
          ":ghost" ["label-dark" :ghost]
          ":question" ["label-cyan" :question]
          ":times" ["label-red" :cross]
          ":unchecked" ["label-teal" :check/normal])
        extra-class (if set? (str badge-color " label-set") badge-color)]
    [:span.badge.rounded-pill.px-3
     {:class (if hover? (str extra-class " label") extra-class)}
     [icon icon-name "m-auto"]]))

(rf/reg-event-fx
 :statement.labels/remove
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels (fn [labels] (-> labels set (disj label))))
         parent-id (-> updated-statement :statement/parent :db/id)]
     {:db (as-> db new-db
            (update-in new-db [:search :schnaq :current :result] #(tools/update-statement-in-list % updated-statement))
            (update-in new-db [:discussion :premises :current parent-id :statement/children]
                       #(tools/update-statement-in-list % updated-statement))
            (update-in new-db [:discussion :premises :current parent-id :meta/answered?]
                       #(shared-tools/answered? (get-in new-db [:discussion :premises :current parent-id]))))
      :fx [(http/xhrio-request db :put "/discussion/statement/label/remove"
                               [:statement.labels.update/success]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))

(rf/reg-event-db
 :statement.labels.update/success
 (fn [db [_ {:keys [statement]}]]
   (update-in db [:discussion :conclusion :selected]
              #(if (= (:db/id %) (:db/id statement))
                 statement
                 %))))

(rf/reg-event-fx
 :statement.labels/add
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels conj label)
         parent-id (-> updated-statement :statement/parent :db/id)]
     {:db (-> db
              (update-in [:search :schnaq :current :result] #(tools/update-statement-in-list % updated-statement))
              (assoc-in [:discussion :premises :current parent-id :meta/answered?] true)
              (update-in [:discussion :premises :current parent-id :statement/children]
                         #(tools/update-statement-in-list % updated-statement)))
      :fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                               [:statement.labels.update/success]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))
