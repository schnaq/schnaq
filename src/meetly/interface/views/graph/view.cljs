(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            [oops.core :refer [oget]]
            [reagent.core :as reagent]))


(def bar-data [{:x 1}
               {:x 2}
               {:x 3}])

(def data
  {:nodes
   [{:id 1, :name "A"}
    {:id 2, :name "B"}
    {:id 3, :name "C"}
    {:id 4, :name "D"}
    {:id 5, :name "E"}
    {:id 6, :name "F"}
    {:id 7, :name "G"}
    {:id 8, :name "H"}
    {:id 9, :name "I"}
    {:id 10, :name "J"}],
   :links
   [{:source 1, :target 2}
    {:source 1, :target 5}
    {:source 1, :target 6}
    {:source 2, :target 3}
    {:source 2, :target 7}
    {:source 3, :target 4}
    {:source 8, :target 3}
    {:source 4, :target 5}
    {:source 4, :target 9}
    {:source 5, :target 10}]})

(def margin {:top 10, :right 30, :bottom 30, :left 40})

(def width (- 400 (:left margin) (:right margin)))
(def height (- 400 (:top margin) (:bottom margin)))

(defn svg [id]
  (-> d3
      (.select (str "#" id))
      (.append "svg")
      (.attr "width" (+ width (:left margin) (:right margin)))
      (.attr "height" (+ height (:top margin) (:bottom margin)))
      (.append "g")
      (.attr "transform" (str "translate("
                              (:left margin) ","
                              (:top margin) ")"))))

(defn link [svg-selected]
  (-> svg-selected
      (.selectAll "line")
      (.data (clj->js (:links data)))
      (.enter)
      (.append "line")
      (.style "stroke" "#aaa")))

(defn node [svg-selected]
  (-> svg-selected
      (.selectAll "circle")
      (.data (clj->js (:nodes data)))
      (.enter)
      (.append "circle")
      (.attr "r" 20)
      (.style "fill" "#69b3a2")))

(defn ticked [link node]
  (fn [] (-> link
             (.attr "x1" (fn [d] (.-x (.-source d))))
             (.attr "y1" (fn [d] (.-y (.-source d))))
             (.attr "x2" (fn [d] (.-x (.-target d))))
             (.attr "y2" (fn [d] (.-y (.-target d)))))

    (-> node
        (.attr "cx" (fn [d]
                      ;(println d)
                      ;(+ (.-x d) 6)
                      (rand 40)
                      ))
        (.attr "cy" (fn [d]
                      ;(+ (.-y d) 6)
                      (rand 40)
                      )))))



;; -----------------------------------------------------------------------------
;; Bars lifecycle and drawing methods

(defn d3-enter [id]
  (let [svg-prepared (svg id)
        link-prepared (link svg-prepared)
        node-prepared (node svg-prepared)
        link-nodes (-> d3
                       (.forceLink)
                       (.id (fn [d] (.-id d)))
                       (.links (clj->js (:links data))))]

    (-> d3
        (.forceSimulation (clj->js (:nodes data)))
        (.force "link" link-nodes)
        (.force "charge" (.strength (.forceManyBody d3) -400))
        (.force "center" (.forceCenter d3 (/ width 2) (/ height 2)))
        (.on "end" (ticked link-prepared node-prepared))))

  #_(-> d3
        (.select (str "#" id " svg .container .bars"))
        (.selectAll "rect")
        (.data (clj->js bar-data))
        (.enter)
        (.append "rect")))

(defn d3-update [id]
  #_(let [data-n (count bar-data)
          rect-height (/ height data-n)
          x-scale (-> d3
                      (.scaleLinear)
                      (.domain #js [0 5])
                      (.range #js [0 width]))]
      (-> (.select d3 (str "#" id " svg .container .bars"))
          (.selectAll "rect")
          (.data (clj->js bar-data))
          (.attr "fill" "green")
          (.attr "x" (x-scale 0))
          (.attr "y" (fn [_ i]
                       (* i rect-height)))
          (.attr "height" (- rect-height 1))
          (.attr "width" (fn [d]
                           (x-scale (aget d "x")))))))

(defn d3-exit [id]
  #_(-> (.select d3 (str "#" id " svg .container .bars"))
        (.selectAll "rect")
        (.data (clj->js bar-data))
        (.exit)
        (.remove)))

(defn d3-did-update [id]
  (d3-enter id)
  (d3-update id)
  (d3-exit id))

(defn d3-did-mount [id]
  (-> (.select d3 (str "#" id " svg .container"))
      (.append "g")
      (.attr "class" "bars"))
  (d3-did-update id))


;; -----------------------------------------------------------------------------
;; Container creates a group to draw the bar into.

(defn d3-container-enter [id]
  (-> (.select d3 (str "#" id " svg"))
      (.append "g")
      (.attr "class" "container")))

(defn d3-container-did-mount [id]
  (d3-container-enter id))


;; -----------------------------------------------------------------------------
;; Visualization lifecycle methods.

(def view-width "100%")
(def view-height "80vh")

(defn viz-component [id]
  [:div
   {:id id}
   [:svg
    {:width view-width
     :height view-height}]])

(defn viz2-did-mount [id]
  ;; order matters here
  (d3-container-did-mount id)
  (d3-did-mount id))

(defn viz-did-update [id]
  (d3-did-update id))

(defn viz [id]
  (reagent/create-class
    {:reagent-render #(viz-component id)
     :component-did-mount #(viz2-did-mount id)
     :component-did-update #(viz-did-update id)}))



(defn view []
  [:div
   [:h1 "Barchart"]
   [viz "barchart"]])