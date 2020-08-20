(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as reagent]))

;; christian

(def data
  [{:date "1-May-12" :close 58.13}
   {:date "30-Apr-12" :close 53.98}
   {:date "27-Apr-12" :close 67.00}
   {:date "26-Apr-12" :close 89.70}
   {:date "25-Apr-12" :close 99.00}
   {:date "24-Apr-12" :close 130.28}
   {:date "23-Apr-12" :close 166.70}
   {:date "20-Apr-12" :close 234.98}
   {:date "19-Apr-12" :close 345.44}
   {:date "18-Apr-12" :close 443.34}
   {:date "17-Apr-12" :close 543.70}
   {:date "16-Apr-12" :close 580.13}
   {:date "13-Apr-12" :close 605.23}
   {:date "12-Apr-12" :close 622.77}
   {:date "11-Apr-12" :close 626.20}
   {:date "10-Apr-12" :close 628.44}
   {:date "9-Apr-12" :close 636.23}
   {:date "5-Apr-12" :close 633.68}
   {:date "4-Apr-12" :close 624.31}
   {:date "3-Apr-12" :close 629.32}
   {:date "2-Apr-12" :close 618.63}
   {:date "30-Mar-12" :close 599.55}
   {:date "29-Mar-12" :close 609.86}
   {:date "28-Mar-12" :close 617.62}
   {:date "27-Mar-12" :close 614.48}])
(def width 1000)
(def height 600)
(def margin {:top 20, :right 20, :bottom 50, :left 70})
(def parseTime (.timeParse d3 "%d-%b-%y"))
(def x (.range (.scaleTime d3) #js [0 width]))
(def y (.range (.scaleLinear d3) #js [height 0]))
(def valueline
  (-> d3
      (.line)
      (.x (fn [d] (x (.-date d))))
      (.y (fn [d] (y (.-close d))))))

(defn svg-builder [node]
  (-> d3
      (.select node)
      (.append "svg")
      (.attr "width" (+ (+ width (:left margin)) (:right margin)))
      (.attr "height" (+ (+ height (:top margin)) (:bottom margin)))
      (.append "g")
      (.attr "transform" (gstring/format "translate(%s, %s)" (:left margin) (:top margin)))))

(def svg
  (svg-builder "body"))

(defn- draw-graph [svg2]
  (let [data (clj->js data)]
    (.forEach
      data
      (fn [d]
        (set! (.-date d) (parseTime (.-date d)))
        (set! (.-close d) (+ (.-close d)))))
    (.domain x (.extent d3 data (fn [d] (.-date d))))
    (.domain y #js [0 (.max d3 data (fn [d] (.-close d)))])
    (->
      ((.-append svg) "path")
      (.data #js [data])
      (.attr "class" "line")
      (.attr "d" valueline))
    (->
      ((.-append svg) "g")
      (.attr "transform" (gstring/format "translate(0, %s)" height))
      (.call (.axisBottom d3 x)))
    (.call (.append svg "g") (.axisLeft d3 y))))

(defn view []
  (reagent/create-class
    {:component-did-mount
     (fn [_comp]
       (draw-graph svg)
       #_(gdom/append (gdom/getElement "graph-div")
                      (.node svg (draw-graph))
                      ))
     :reagent-render
     (fn []
       [:div
        [:h1 "hello"]
        [:div#graph-div]])}))



;(.appendChild
;  (.createElement js/document "h1")
;  (.createTextNode js/document "I did it"))

;(draw-graph)
;[:style ".line {\n  fill: none;\n  stroke: steelblue;\n  stroke-width: 2px;\n}"]
