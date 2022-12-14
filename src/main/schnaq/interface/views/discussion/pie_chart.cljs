(ns schnaq.interface.views.discussion.pie-chart
  (:require ["chart.js/auto"]
            ["react-chartjs-2" :refer [Chart]]
            [schnaq.interface.components.colors :refer [colors]]))

(defn create-vote-chart-data
  "Creates a list of voting data for an react-vis pie chart."
  [statement]
  (let [up-votes (get statement :statement/upvotes 0)
        down-votes (get statement :statement/downvotes 0)
        neutral-votes (if (and (zero? up-votes) (zero? down-votes)) 1 0)]
    [:> Chart {:type "doughnut"
               :options {:events [] :responsive true}
               :data {:datasets [{:label "Statement votes",
                                  :data [neutral-votes, up-votes, down-votes]
                                  :backgroundColor [(:neutral/medium colors)
                                                    (:positive/default colors)
                                                    (:negative/default colors)]
                                  :hoverOffset 4
                                  :cutout "70%"}]}}]))
