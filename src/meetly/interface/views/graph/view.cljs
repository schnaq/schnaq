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

(defn d3-enter [id]
  (-> d3
      (.select (str "#" id " svg .container .bars"))
      (.selectAll "rect")
      (.data (clj->js data))
      (.enter)
      (.append "rect")))

(defn d3-update [id]
  (let [data-n (count data)
        rect-height (/ height data-n)
        x-scale (-> d3
                    (.scaleLinear)
                    (.domain #js [0 5])
                    (.range #js [0 width]))]
    (-> (.select d3 (str "#" id " svg .container .bars"))
        (.selectAll "rect")
        (.data (clj->js data))
        (.attr "fill" "green")
        (.attr "x" (x-scale 0))
        (.attr "y" (fn [_ i]
                     (* i rect-height)))
        (.attr "height" (- rect-height 1))
        (.attr "width" (fn [d]
                         (x-scale (aget d "x")))))))

(defn d3-exit [id]
  (-> (.select d3 (str "#" id " svg .container .bars"))
      (.selectAll "rect")
      (.data (clj->js data))
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