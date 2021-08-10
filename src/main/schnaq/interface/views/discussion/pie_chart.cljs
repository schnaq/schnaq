(ns schnaq.interface.views.discussion.pie-chart
  (:require ["chart.js"]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :as config]))


(defn create-vote-chart-data
  "Creates a list of voting data for an react-vis pie chart."
  [statement]
  (let [up-votes (get statement :meta/upvotes 0)
        down-votes (get statement :meta/downvotes 0)
        neutral-votes (if (and (zero? up-votes) (zero? down-votes)) 1 0)]
    {:type "doughnut"
     :options {:events [] :responsive true}
     :data {:datasets [{:label "Pie Chart Data",
                        :data [neutral-votes, up-votes, down-votes]
                        :backgroundColor [config/neutral-color
                                          config/upvote-color
                                          config/downvote-color]
                        :hoverOffset 4
                        :cutout "70%"}]}}))

(defn pie-chart-component
  [_data]
  (reagent/create-class
    {:component-did-mount
     (fn [comp]
       (let [[_ chart-data] (reagent/argv comp)]
         (js/Chart. (rdom/dom-node comp) (clj->js chart-data))))
     :display-name "chart-js-component"
     :reagent-render (fn [_data] [:canvas {:style {:margin-top -10}}])}))