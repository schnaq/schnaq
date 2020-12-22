(ns schnaq.interface.views.discussion.history
  (:require [re-frame.core :as rf]))

(defn- rewind-history
  "Rewinds a history until the last time statement-id was current."
  [history statement-id]
  (loop [history history]
    (if (or (= (:db/id (last history)) statement-id)
            (empty? history))
      (vec history)
      (recur (butlast history)))))

(rf/reg-event-db
  :discussion.history/push
  ;; IMPORTANT: Since we do not control what happens at the browser back button, pushing anything
  ;; that is already present in the history, will rewind the history to said place.
  (fn [db [_ statement]]
    (let [all-entries (-> db :history :full-context)
          history-ids (into #{} (map :db/id all-entries))]
      (if (and statement (contains? history-ids (:db/id statement)))
        (assoc-in db [:history :full-context] (rewind-history all-entries (:db/id statement)))
        (update-in db [:history :full-context] conj statement)))))

(rf/reg-event-db
  :discussion.history/clear
  (fn [db _]
    (assoc-in db [:history :full-context] [])))

(rf/reg-event-fx
  :discussion.history/time-travel
  (fn [{:keys [db]} [_ times]]
    ;; Only continue when default value (nil - go back one step) is set or we go back more than 0 steps
    (when (or (nil? times) (< 0 times))
      (let [steps-back (or times 1)
            before-time-travel (get-in db [:history :full-context])
            keep-n (- (count before-time-travel) steps-back)
            after-time-travel (vec (take keep-n before-time-travel))
            {:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
        (if (>= 0 keep-n)
          {:fx [[:dispatch [:discussion.history/clear]]
                [:dispatch [:navigation/navigate :routes.discussion/start {:id id :share-hash share-hash}]]]}
          {:db (assoc-in db [:history :full-context] after-time-travel)
           :fx [[:dispatch [:navigation/navigate :routes.discussion.select/statement
                            {:id id :share-hash share-hash :statement-id (:db/id (last after-time-travel))}]]]})))))

(rf/reg-sub
  :discussion-history
  (fn [db _]
    (get-in db [:history :full-context])))