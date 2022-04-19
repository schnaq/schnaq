(ns schnaq.interface.components.colors)

(def colors
  {:positive/dark "#052740"
   :positive/default "#1292ee"
   :positive/light "#4cacf4"
   :positive/selected "#0181dd"
   :secondary "#ff9e0d"
   :negative/default "#ff772d"
   :negative/selected "#fe661e"
   :neutral/dark "#adb5bd"
   :neutral/medium "#adb5bd"
   :white "#ffffff"})

(def ^:private graph-colors
  ;; Generated with https://learnui.design/tools/data-color-picker.html with primary as starting point
  ;; and secondary as endpoint
  ["#1292ee" "#8389f2" "#c37ce5" "#f36cc8" "#ff64a1" "#ff6c75" "#ff8247" "#ff9e0d"])

(defn get-graph-color
  "Returns a graph color, given an index."
  [index]
  (get graph-colors (mod index 8)))
