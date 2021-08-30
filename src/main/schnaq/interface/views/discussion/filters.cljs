(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internaly. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [oops.core :refer [oget oget+]]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.labels :as statement-labels]))

(defn- set-selected-option
  "Helper function to set the correct temp atom value for a selection."
  [event store]
  (let [options (oget event :target :options)
        selection-index (str (oget event :target :selectedIndex))]
    (reset! store (oget+ options selection-index :value))))

(defn- label-selector
  "A component which helps selecting the labels for a filter."
  [selected-label]
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
    [:span.m-auto "x"]]])

(defn- add-filter-selection
  "A small compontent for adding new filters."
  []
  (let [current-selection (r/atom "labels")
        selected-label (r/atom nil)]
    (fn []
      [:section.border-bottom.pb-2
       [:form.text-left
        {:on-submit #(jsw/prevent-default %)}
        [:div.form-group
         [:label {:for :add-filter-menu}
          (labels :filters.label/filter-for)]
         [:select#add-filter-menu.mr-1.form-control
          {:on-change #(set-selected-option % current-selection)}
          [:option {:value :labels} (labels :filters.option.labels/text)]
          [:option {:value :type} (labels :filters.option.type/text)]
          [:option {:value :votes} (labels :filters.option.votes/text)]]]
        (case @current-selection
          "labels"
          [:<>
           [:div.form-row.pb-3
            [:div.col-auto
             [:select#filter-labels-selection.mr-1.form-control
              [:option {:value :includes} (labels :filters.option.labels/includes)]
              [:option {:value :excludes} (labels :filters.option.labels/excludes)]]]
            [:div.col-auto
             [label-selector selected-label]]]]
          "")
        [:button.btn.btn-outline-dark.mr-2
         [:i {:class (fa :plus)}] " " (labels :filters.add/button)]]])))

(defn- default-menu
  "The default filter menu that is shown to the user."
  []
  [:div
   [add-filter-selection]])

(defn filter-button
  "A button opening the default filters on click."
  []
  (let [active-filters? false]
    ;; TODO add flag for when filters are active.
    [tooltip/html
     [default-menu]
     [:button.btn.btn-outline-primary.mr-2.h-100
      {:class (when active-filters? "active")}
      (labels :badges.filters/button)]
     {:hideOnClick :toggle}]))
