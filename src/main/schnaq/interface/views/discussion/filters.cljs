(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internaly. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [clojure.set :as cset]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.labels :as statement-labels]))

;; TODO add button to clear filters

(defn- set-selected-option
  "Helper function to set the correct temp atom value for a selection."
  [event store]
  (reset! store (tools/get-selection-from-event event)))

(defn- label-selections
  "Selection-options for label-type filters."
  [selected-label]
  [:<>
   [:div.form-row.pb-3
    [:div.col-auto
     [:select#filter-labels-selection.mr-1.form-control
      [:option {:value :includes} (labels :filters.option.labels/includes)]
      [:option {:value :excludes} (labels :filters.option.labels/excludes)]]]
    [:div.col-auto
     [:div.btn-group
      {:role "group"}
      [tooltip/html
       (for [label shared-config/allowed-labels]
         [:span.mr-3
          {:key (str "label-option-" label)
           :on-click #(reset! selected-label label)}
          [statement-labels/build-label label]])
       [:button#filter-labels-label.form-control
        (if (shared-config/allowed-labels @selected-label)
          (with-meta
            [statement-labels/build-label @selected-label]
            {:key (str "label-option-" @selected-label)})
          [:span.badge.badge-pill.badge-transparent "–––"])]]
      [:button.btn.btn-dark
       {:on-click #(reset! selected-label nil)}
       [:span.m-auto "x"]]]]]])

(defn- add-filter-selection
  "A small compontent for adding new filters."
  []
  (let [current-selection (r/atom "labels")
        selected-label (r/atom nil)]
    (fn []
      [:section.border-bottom.pb-2.text-left
       [:div.form-group
        [:label {:for :add-filter-menu}
         (labels :filters.label/filter-for)]
        [:select#add-filter-menu.mr-1.form-control
         {:on-change #(set-selected-option % current-selection)}
         [:option {:value :labels} (labels :filters.option.labels/text)]
         [:option {:value :type} (labels :filters.option.type/text)]
         [:option {:value :votes} (labels :filters.option.votes/text)]]]
       (case @current-selection
         "labels" [label-selections selected-label]
         "")
       [:button.btn.btn-outline-dark.mr-2
        {:on-click #(when @selected-label
                      (rf/dispatch [:filters.activate/labels
                                    (tools/get-current-selection (gdom/getElement "filter-labels-selection"))
                                    @selected-label]))}
        [:i {:class (fa :plus)}] " " (labels :filters.add/button)]])))

(defn- default-menu
  "The default filter menu that is shown to the user."
  []
  [:div
   [add-filter-selection]])

(defn filter-button
  "A button opening the default filters on click."
  []
  (let [active-filters? @(rf/subscribe [:filters/active?])]
    [tooltip/html
     [default-menu]
     [:button.btn.btn-outline-primary.mr-2.h-100
      {:class (when active-filters? "btn-outline-secondary active")}
      (labels :badges.filters/button)]
     {:hideOnClick :toggle}]))

(rf/reg-event-db
  :filters.activate/labels
  (fn [db [_ criteria label]]
    (let [new-filter {:type :labels
                      :criteria (keyword criteria)
                      :label label}]
      (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))))

(rf/reg-sub
  :filters/active
  (fn [db _]
    (get-in db [:discussion :filters] #{})))

(rf/reg-sub
  :filters/active?
  (fn [_]
    (rf/subscribe [:filters/active]))
  (fn [active-filters _]
    (seq active-filters)))

(defn- filter-to-fn
  "Returns the corresponding filter-fn for a data-representation of a filter."
  [{:keys [type criteria label]}]
  (cond
    (= type :labels)
    (let [coll-fn (if (= criteria :includes) filter remove)]
      (fn [statements] (coll-fn #(contains? (set (:statement/labels %)) label) statements)))))

(defn filter-statements
  "Accepts a collection of statements and filters and applies them to the collection."
  [statements filters]
  (let [filter-fns (map filter-to-fn filters)]
    ;; Apply every filter-function to the statements before returning them
    (reduce (fn [statements filter-fn] (filter-fn statements)) statements filter-fns)))
