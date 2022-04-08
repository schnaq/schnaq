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

(defn- store-label-change
  "Differentiates if the statement, to which the label is added / removed, is the currently shown as a parent
  or a child."
  [db updated-statement]
  (let [parent-id (-> updated-statement :statement/parent :db/id)
        parent-statement (get-in db [:schnaq :statements parent-id])
        statement-in-store (get-in db [:schnaq :statements (:db/id updated-statement)])
        statement-answered? (shared-tools/answered?
                             {:statement/children (tools/update-statement-in-list
                                                   (:statement/children parent-statement) updated-statement)})]
    (cond-> db
      ;; Statement is there as a child update it as such.
      parent-statement (update-in [:schnaq :statements parent-id :statement/children]
                                  #(tools/update-statement-in-list % updated-statement))
      parent-statement (assoc-in [:schnaq :statements parent-id :meta/answered?] statement-answered?)
      ;; If the statement is itself in the store update it as well
      statement-in-store (update-in [:schnaq :statements (:db/id updated-statement)] updated-statement))))

(rf/reg-event-fx
 :statement.labels/remove
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels (fn [labels] (-> labels set (disj label) vec)))]
     {:db (store-label-change db updated-statement)
      :fx [(http/xhrio-request db :put "/discussion/statement/label/remove"
                               [:statement.labels.update/success]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))

(rf/reg-event-db
 :statement.labels.update/success
 ;; TODO change this with a generic update-statement fn
 (fn [db [_ {:keys [statement]}]]
   (assoc-in db [:schnaq :statements (:db/id statement)] statement)))

(rf/reg-event-fx
 :statement.labels/add
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels conj label)]
     {:db (store-label-change db updated-statement)
      :fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                               [:statement.labels.update/success]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))
