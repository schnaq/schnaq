(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            ["/graph" :as schnaqd3]
            [meetly.interface.views.graph.test-data :as test-data]
            [reagent.core :as reagent]))

(defn viz [id]
  (reagent/create-class
    {:reagent-render (fn [] [:svg {:id id} "Graph, lel"])
     :component-did-mount (fn []
                            (let [width 1200
                                  height 600]
                              (->
                                (schnaqd3/SchnaqD3. d3 (str "#" id) (clj->js test-data/miserables))
                                (.setSize width height)
                                #_(.replaceData (clj->js test-data/short-miserables) width height 10))))}))

(defn view []
  [:div.container
   [:h1 "Barchart"]
   [viz "viz"]])