(ns schnaq.interface.views.feed.filters
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn- default-menu
  "The default filter menu that is shown to the user."
  []
  [:<>
   #_[add-filter-selection]
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
