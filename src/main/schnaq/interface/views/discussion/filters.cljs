(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internally. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [clojure.set :as cset]
            [re-frame.core :as rf]))

(defn- register-new-filter [db new-filter]
  (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))

(defn- remove-filter [db old-filter]
  (update-in db [:discussion :filters] disj old-filter))

(rf/reg-event-db
 :filters.answered/set
 ;; Show only answered (true) or unanswered (false) statements. nil shows all.
 (fn [db [_ answered?]]
   (cond-> (-> db
               (remove-filter {:type :answered? :criteria true})
               (remove-filter {:type :answered? :criteria false}))
     (some? answered?) (register-new-filter {:type :answered? :criteria answered?}))))

(rf/reg-event-db
 :filters.activate/questions
 (fn [db _]
   (let [new-filter {:type :question}]
     (register-new-filter db new-filter))))

(rf/reg-event-db
 :filters.deactivate/questions
 (fn [db _]
   (let [question-filter {:type :question}]
     (remove-filter db question-filter))))

(rf/reg-event-db
 :filters/clear
 (fn [db _]
   (assoc-in db [:discussion :filters] #{})))

(rf/reg-sub
 :filters/active
 (fn [db _]
   (get-in db [:discussion :filters] #{})))

(rf/reg-sub
 :filters/answered?
 ;; Show whether the answered? filter is active.
 (fn [db [_ toggle]]
   (contains? (get-in db [:discussion :filters]) {:type :answered? :criteria toggle})))

(rf/reg-sub
 :filters/questions?
 ;; Shows whether the questions filter is active
 (fn [db _]
   (contains? (get-in db [:discussion :filters]) {:type :question})))
