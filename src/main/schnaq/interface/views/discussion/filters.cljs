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
 :filters/answered?
 ;; Show whether the answered? filter is active.
 (fn [db _]
   (contains? (get-in db [:discussion :filters]) {:type :answered? :criteria true})))

(rf/reg-sub
 :filters/active?
 :<- [:filters/active]
 (fn [active-filters _]
   (seq active-filters)))
