(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internally. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [clojure.set :as cset]
            [re-frame.core :as rf]
            [schnaq.interface.components.schnaq :as sc]))

(defn filter-answered-statements
  "Show buttons to toggle between answered / unanswered statements."
  []
  [sc/schnaq-statement-filter-button-group
   [{:on-click #(rf/dispatch [:filters/clear])
     :label-key :filters.option.answered/all}
    {:on-click (fn [] (rf/dispatch [:filters/clear])
                 (rf/dispatch [:filters.activate/answered? true]))
     :label-key :filters.option.answered/answered}
    {:on-click (fn [] (rf/dispatch [:filters/clear])
                 (rf/dispatch [:filters.activate/answered? false]))
     :label-key :filters.option.answered/unanswered}]])


;; -----------------------------------------------------------------------------


(defn- register-new-filter [db new-filter]
  (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))

(defn- remove-filter [db old-filter]
  (update-in db [:discussion :filters] disj old-filter))

(rf/reg-event-db
 :filters.activate/answered?
 (fn [db [_ toggle]]
   (let [new-filter {:type :answered?
                     :criteria toggle}]
     (register-new-filter db new-filter))))

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

(rf/reg-sub
 :filters/active?
 :<- [:filters/active]
 (fn [active-filters _]
   (seq active-filters)))
