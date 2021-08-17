(ns schnaq.interface.notification.events
  (:require [clojure.set :refer [union]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]))

(rf/reg-sub
  :visited/statements
  (fn [db [_ discussion-hash]]
    (get-in db [:visited :statement-ids discussion-hash])))

(rf/reg-event-db
  :notification/set-visited-statements
  (fn [db [_ discussion-hash statement premises]]
    (let [visited (set (conj (map :db/id premises) (:db/id statement)))]
      (update-in db [:visited :statement-ids discussion-hash] #(set (concat %1 %2)) visited))))

(rf/reg-event-db
  :visited.save-statement-ids/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:visited :statement-ids] (:discussion/visited-statement-ids local-storage))))

(rf/reg-event-fx
  :visited.statement-ids/to-localstorage
  (fn [{:keys [db]} [_]]
    (let [statement-ids (get-in db [:visited :statement-ids])
          visited-statement-ds (merge-with union (:discussion/statement-ids local-storage) statement-ids)]
      {:fx [[:localstorage/assoc [:discussion/visited-statement-ids visited-statement-ds]]
            [:dispatch [:visited.save-statement-ids/store-hashes-from-localstorage]]]})))