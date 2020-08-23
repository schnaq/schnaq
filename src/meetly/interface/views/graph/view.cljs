(ns meetly.interface.views.graph.view
  (:require ["/js/schnaqd3/graph" :as schnaqd3]
            [meetly.interface.views.graph.test-data :as test-data]
            [reagent.core :as reagent]))

(defn viz [id]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id} "Graph, lel"])
     :component-did-mount (fn []
                            (->
                              (schnaqd3/SchnaqD3. (str "#" id) (clj->js test-data/miserables))
                              (.setSize 1200 600)
                              (.replaceData (clj->js test-data/short-miserables))))}))

(defn view []
  [:div.container
   [:h1 "Barchart"]
   [viz "viz"]])