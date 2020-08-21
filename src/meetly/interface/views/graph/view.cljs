(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            ["/js/schnaqd3/graph" :as schnaqd3]
            [meetly.interface.views.graph.test-data :as test-data]
            [reagent.core :as reagent]))

(defn viz [id]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id} "Graph, lel"])
     :component-did-mount (fn []
                            (schnaqd3/drawGraph d3 (str "#" id) (clj->js test-data/miserables))
                            (js/setTimeout #(schnaqd3/setSize d3 (str "#" id) 800 800) 5000))}))

(defn view []
  [:div.container
   [:h1 "Barchart"]
   [viz "viz"]])