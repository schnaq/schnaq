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

(defn- store-statement
  "Store updated statement in app-db.
  Differentiates if the statement, to which the label is added / removed, is the
  currently selected conclusion or it is a child of a statement inside the 
  statement list."
  [db updated-statement]
  (let [parent-id (-> updated-statement :statement/parent :db/id)
        parent-in-current-premises? (or (not (nil? (get-in db [:schnaq :statements parent-id])))
                                        ;; TODO [:schnaq :qa :search :results parent-id] sollte auch in :schnaq :statements liegen
                                        (not (nil? (get-in db [:schnaq :qa :search :results parent-id]))))]
    (if parent-in-current-premises?
      (let [db-updated-children
            (-> db
                (update-in [:schnaq :statements parent-id :statement/children]
                           #(tools/update-statement-in-list % updated-statement))
                (update-in [:schnaq :qa :search :results parent-id :statement/children]
                           #(tools/update-statement-in-list % updated-statement)))
            current-parent (get-in db-updated-children [:schnaq :statements parent-id])
            current-qa-parent (get-in db-updated-children [:schnaq :qa :search :results parent-id])]
        (-> db-updated-children
            (update-in [:schnaq :statements parent-id :meta/answered?]
                       #(shared-tools/answered? current-parent))
            (update-in [:schnaq :qa :search :results parent-id :meta/answered?]
                       #(shared-tools/answered? current-qa-parent))))
      (-> db
          (assoc-in [:schnaq :statements (:db/id updated-statement)] updated-statement)
          (assoc-in [:schnaq :qa :search :results (:db/id updated-statement)] updated-statement)))))

(rf/reg-event-fx
 :statement.labels/remove
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels (fn [labels] (-> labels set (disj label) vec)))]
     {:db (store-statement db updated-statement)
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
   (let [update-fn #(if (= (:db/id %) (:db/id statement))
                      statement
                      %)]
     (update-in db [:schnaq :qa :search :results] update-fn))))

(rf/reg-event-fx
 :statement.labels/add
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels conj label)]
     {:db (store-statement db updated-statement)
      :fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                               [:statement.labels.update/success]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))
