(ns schnaq.interface.views.schnaq.summary
  "All views and events important to extractive summaries can be found here."
  (:require [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(defn- user-summary-view
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-nav
     ;; todo labelize
     {:page/heading "Summaries"
      :page/subheading "See the discussion in a few sentences"}
     current-discussion
     [:div "Hallo"]]))

(defn public-user-view []
  [user-summary-view])