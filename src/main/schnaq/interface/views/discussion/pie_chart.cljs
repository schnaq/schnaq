(ns schnaq.interface.views.discussion.pie-chart
  (:require ["chart.js"]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :refer [colors]]))


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
                        :backgroundColor [(:neutral/medium colors)
                                          (:positive/default colors)
                                          (:negative/default colors)]
                        :hoverOffset 4
                        :cutout "70%"}]}}))

(defn pie-chart-component
  [_data]
  (let [pie-chart (reagent/atom nil)]
    (reagent/create-class
      {:display-name "chart-js-component"
       :component-did-mount
       (fn [comp]
         (let [[_ chart-data] (reagent/argv comp)]
           (reset! pie-chart (js/Chart. (rdom/dom-node comp) (clj->js chart-data)))))
       :component-did-update
       (fn [comp]
         (let [[_ chart-data] (reagent/argv comp)]
           (.destroy @pie-chart)
           (reset! pie-chart (js/Chart. (rdom/dom-node comp) (clj->js chart-data)))))
       :reagent-render (fn [_data] [:canvas {:style {:margin-top -10}}])})))