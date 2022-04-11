(ns schnaq.interface.views.discussion.labels
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

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
         updated-statement (update statement :statement/labels (fn [labels] (-> labels set (disj label) vec)))]
     {:db (assoc-in db [:schnaq :statements (:db/id updated-statement)] updated-statement)
      :fx [(http/xhrio-request db :put "/discussion/statement/label/remove"
                               [:statement/update]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))

(rf/reg-event-fx
 :statement.labels/add
 (fn [{:keys [db]} [_ statement label]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         updated-statement (update statement :statement/labels conj label)]
     {:db (assoc-in db [:schnaq :statements (:db/id updated-statement)] updated-statement)
      :fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                               [:statement/update]
                               {:share-hash share-hash
                                :statement-id (:db/id statement)
                                :label label
                                :display-name (tools/current-display-name db)})]})))
