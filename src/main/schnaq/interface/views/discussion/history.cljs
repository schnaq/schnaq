(ns schnaq.interface.views.discussion.history
  (:require [re-frame.core :as rf]
            [schnaq.shared-toolbelt :as stools]))

(defn- rewind-history
  "Rewinds a history until the last time statement-id was current."
  [history statement-id]
  (loop [history history]
    (if (or (= (last history) statement-id)
            (empty? history))
      (vec history)
      (recur (butlast history)))))

(rf/reg-event-db
 :discussion.history/push
 ;; IMPORTANT: Since we do not control what happens at the browser back button, pushing anything
 ;; that is already present in the history, will rewind the history to said place.
 (fn [db [_ {:keys [db/id]}]]
   (let [all-entries (get-in db [:history :full-context])]
     (if (contains? (set all-entries) id)
       (assoc-in db [:history :full-context] (rewind-history all-entries id))
       (update-in db [:history :full-context] conj id)))))

(rf/reg-event-db
 :discussion.history/clear
 (fn [db _]
   (assoc-in db [:history :full-context] [])))

(rf/reg-event-fx
 :discussion.history/time-travel
 (fn [{:keys [db]} [_ times]]
   ;; default value (nil - go back one step)
   ;; 0 steps is used in the search view to go back to the last statement
   (when (or (nil? times) (<= 0 times))
     (let [steps-back (or times 1)
           before-time-travel (get-in db [:history :full-context])
           keep-n (- (count before-time-travel) steps-back)
           after-time-travel (vec (take keep-n before-time-travel))
           {:keys [share-hash]} (get-in db [:current-route :parameters :path])]
       (if (>= 0 keep-n)
         {:fx [[:dispatch [:discussion.history/clear]]
               [:dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}]]]}
         {:db (assoc-in db [:history :full-context] after-time-travel)
          :fx [[:dispatch [:navigation/navigate :routes.schnaq.select/statement
                           {:share-hash share-hash :statement-id (:db/id (last after-time-travel))}]]]})))))

(rf/reg-sub
 :discussion-history
 (fn [db _]
   (let [history-ids (get-in db [:history :full-context] [])]
     (stools/select-values (get-in db [:schnaq :statements] history-ids)))))
