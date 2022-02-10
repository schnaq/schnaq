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
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.views.discussion.logic :as discussion-logic]))

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

(rf/reg-event-db
 :filters.activate/answered?
 (fn [db [_ toggle]]
   (let [new-filter {:type :answered?
                     :criteria toggle}]
     (register-new-filter db new-filter))))

(rf/reg-event-db
 :filters/clear
 (fn [db _]
   (assoc-in db [:discussion :filters] #{})))

(rf/reg-sub
 :filters/active
 (fn [db _]
   (get-in db [:discussion :filters] #{})))

(rf/reg-sub
 :filters/active?
 :<- [:filters/active]
 (fn [active-filters _]
   (seq active-filters)))

(defn- filter-to-fn
  "Returns the corresponding filter-fn for a data-representation of a filter."
  [{:keys [type criteria label statement-type votes-number]} local-votes]
  (case type
    :answered? (fn [statements] ((if criteria filter remove) :meta/answered? statements))
    :labels
    (let [coll-fn (if (= criteria :includes) filter remove)]
      (fn [statements] (coll-fn #(contains? (set (:statement/labels %)) label) statements)))
    :type
    (let [coll-fn (if (= criteria :is) filter remove)]
      (fn [statements] (coll-fn #(or (= (:statement/type %) statement-type)
                                     ;; Account for starting statements as neutral
                                     (and (= statement-type :statement.type/neutral) (nil? (:statement/type %))))
                                statements)))
    :votes
    ;; Calling symbol on the string does not help. Other solutions are hacky.
    (let [comp-fn (case criteria ">" > "=" = "<" <)
          ;; The deref needs to happen here, because it cant happen in a lazy seq.
          ;; Derefing in the component does not update the filters, when votes change.
          votes @local-votes]
      (fn [statements] (filter #(comp-fn (discussion-logic/calculate-votes % votes) votes-number) statements)))
    identity))

(defn filter-statements
  "Accepts a collection of statements and filters and applies them to the collection."
  [statements filters votes]
  (let [filter-fns (map #(filter-to-fn % votes) filters)]
    ;; Apply every filter-function to the statements before returning them
    (reduce (fn [statements filter-fn] (filter-fn statements)) statements filter-fns)))
