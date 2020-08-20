(ns meetly.interface.views.graph.view
  (:require ["d3" :as d3]
            [reagent.core :as reagent]))

(def width 300)
(def height 400)

(def data [{:x 1}
           {:x 2}
           {:x 3}])


;; -----------------------------------------------------------------------------
;; Bars lifecycle and drawing methods

(defn bars-enter []
  (-> (.select d3 "#barchart svg .container .bars")
      (.selectAll "rect")
      (.data (clj->js data))
      (.enter)
      (.append "rect")))

(defn bars-update []
  (let [data-n (count data)
        rect-height (/ height data-n)
        x-scale (-> (.scaleLinear d3)
                    (.domain #js [0 5])
                    (.range #js [0 width]))]
    (-> (.select d3 "#barchart svg .container .bars")
        (.selectAll "rect")
        (.data (clj->js data))
        (.attr "fill" "green")
        (.attr "x" (x-scale 0))
        (.attr "y" (fn [_ i]
                     (* i rect-height)))
        (.attr "height" (- rect-height 1))
        (.attr "width" (fn [d]
                         (x-scale (aget d "x")))))))

(defn bars-exit []
  (-> (.select d3 "#barchart svg .container .bars")
      (.selectAll "rect")
      (.data (clj->js data))
      (.exit)
      (.remove)))

(defn bars-did-update []
  (bars-enter)
  (bars-update)
  (bars-exit))

(defn bars-did-mount []
  (-> (.select d3 "#barchart svg .container")
      (.append "g")
      (.attr "class" "bars"))
  (bars-did-update))


;; -----------------------------------------------------------------------------
;; Container creates a group to draw the bar into.

(defn container-enter []
  (-> (.select d3 "#barchart svg")
      (.append "g")
      (.attr "class" "container")))

(defn container-did-mount []
  (container-enter))


;; -----------------------------------------------------------------------------
;; Visualization lifecycle methods.

(defn viz-component []
  [:div#barchart
   [:svg
    {:width width
     :height height}]])

(defn viz-did-mount []
  ;; order matters here
  (container-did-mount)
  (bars-did-mount))

(defn viz-did-update []
  (bars-did-update))

(defn viz []
  (reagent/create-class
    {:reagent-render #(viz-component)
     :component-did-mount #(viz-did-mount)
     :component-did-update #(viz-did-update)}))

(defn view []
  [:div
   [:h1 "Barchart"]
   [viz]])