(ns schnaq.interface.views.feed.filters
  (:require [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn- state-selections
  "Selection-options for type filters."
  []
  [:div.form-row.pb-3
   [:div.col-auto
    [:select#filter-state-selection.mr-1.form-control
     [:option {:value :is} (labels :filters.option.type/is)]
     [:option {:value :is-not} (labels :filters.option.type/is-not)]]]
   [:div.col-auto
    [:select#filter-state.mr-1.form-control
     ;; Needs to be string, otherwise ns will be stripped
     [:option {:value "discussion.state/open"} (labels :discussion.add.button/neutral)]
     [:option {:value "statement.type/attack"} (labels :discussion.add.button/attack)]
     [:option {:value "statement.type/support"} (labels :discussion.add.button/support)]]]])

(defn- add-filters
  "A small component for adding new filters."
  []
  (let [current-selection (r/atom "state")
        selected-label (r/atom nil)]
    (fn []
      [:section.border-bottom.pb-2.text-left
       [:div.form-group
        [:label {:for :add-filter-menu}
         (labels :filters.label/filter-for)]
        [:select#add-filter-menu.mr-1.form-control
         {:on-change #(reset! current-selection (tools/get-selection-from-event %))}
         [:option {:value :state} (labels :filters.discussion.option.state/label)]]]
       (case @current-selection
         "state" [state-selections])
       [:button.btn.btn-outline-dark.mr-2
        {:on-click #(case @current-selection
                      "state"
                      (println "placeholder"))}
        [:i {:class (fa :plus)}] " " (labels :filters.add/button)]])))

(defn- default-menu
  "The default filter menu that is shown to the user."
  []
  [:<>
   [add-filters]
   #_[active-filters]
   (when (< 1 (count @(rf/subscribe [:filters/active])))
     [:button.btn.btn-outline-secondary.text-center
      {:on-click #(rf/dispatch [:filters/clear])}
      (labels :filters.buttons/clear)])])

(defn filter-button
  "A button opening the default filters on click."
  []
  (let [active-filters? @(rf/subscribe [:filters/active?])]
    [tooltip/html
     [default-menu]
     [:span.ml-2.pl-1.border-left
      [:button.btn.btn-outline-primary.btn-sm.mx-1
       {:class (when active-filters? "btn-outline-secondary active")}
       (labels :badges.filters/button)]]
     {:hideOnClick :toggle}]))
