(ns schnaq.interface.analytics.charts
  (:require ["react-chartjs-2" :refer [Chart]]))

(defn regular
  "Creates a regular chart, that can be used for most visualisations."
  [chart-name labels data]
  [:> Chart {:type "line"
             :data {:labels labels
                    :datasets [{:label chart-name
                                :data data
                                :borderColor "#1292ee"
                                :backgroundColor "#1292ee"}]}}])
