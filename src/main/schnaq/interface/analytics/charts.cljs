(ns schnaq.interface.analytics.charts
  (:require ["react-chartjs-2" :refer [Chart]]
            [schnaq.interface.components.colors :refer [colors]]))

(defn regular
  "Creates a regular chart, that can be used for most visualisations."
  [chart-name labels data]
  [:> Chart {:type "line"
             :data {:labels labels
                    :datasets [{:label chart-name
                                :data data
                                :borderColor (:positive/default colors)
                                :backgroundColor (:positive/default colors)}]}}])
