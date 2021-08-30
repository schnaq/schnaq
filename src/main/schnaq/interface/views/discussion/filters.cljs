(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internaly. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn default-menu
  "The default filter menu that is shown to the user."
  []
  [:div "Here be filters"])

(defn filter-button
  "A button opening the default filters on click."
  []
  (let [active-filters? false]
    ;; TODO add flag for when filters are active.
    [tooltip/html
     [default-menu]
     [:button.btn.btn-outline-primary.mr-2.h-100
      {:class (when active-filters? "active")}
      (labels :badges.filters/button)]]))
