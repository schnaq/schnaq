(ns schnaq.interface.views.feed.filters
  (:require [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn- add-filters
  "A small component for adding new filters."
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
         "type" [type-selections]
         "votes" [vote-selections]
         "")
       [:button.btn.btn-outline-dark.mr-2
        {:on-click #(case @current-selection
                      "labels"
                      (when @selected-label
                        (rf/dispatch [:filters.activate/labels
                                      (tools/get-current-selection (gdom/getElement "filter-labels-selection"))
                                      @selected-label]))
                      "type"
                      (rf/dispatch [:filters.activate/type
                                    (tools/get-current-selection (gdom/getElement "filter-type-selection"))
                                    (tools/get-current-selection (gdom/getElement "filter-type-type"))])
                      "votes"
                      (rf/dispatch [:filters.activate/votes
                                    (tools/get-current-selection (gdom/getElement "filter-votes-selection"))
                                    (.-value (gdom/getElement "filter-votes-number"))]))}
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
