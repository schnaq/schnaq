(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            ["/js/schnaqd3/graph" :as schnaqd3]
            [oops.core :refer [oget]]
            [reagent.core :as reagent]))

(defn viz [id]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id} "Graph, lel"])
     :component-did-mount #(schnaqd3/drawGraph "#viz")}))

(defn view []
  [:div.container
   [:h1 "Barchart"]
   [viz "viz"]])