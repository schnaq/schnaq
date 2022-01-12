(ns schnaq.interface.components.colors
  (:require [schnaq.config.shared :as shared-config]))

(def colors
  (cond->
    {:positive/dark "#052740"
     :positive/default "#1292ee"
     :positive/light "#4cacf4"
     :positive/selected "#0181dd"
     :secondary "#ff9e0d"
     :negative/default "#ff772d"
     :negative/selected "#fe661e"
     :neutral/dark "#adb5bd"
     :neutral/medium "#adb5bd"
     :white "#ffffff"}
    ;; Add wetog colors. Dispatch further, should we have more embeddings
    shared-config/embedded?
    (merge {:positive/dark "#239580"
            :positive/default "#2cbaa0"
            :positive/light "#56c8b3"
            :positive/selected "#20c997"
            :negative/default "#ff0101"
            :negative/selected "#dc3545"
            :neutral/dark "#170051"
            :neutral/medium "#2a1861"})))

(def graph-colors
  ;; Generated with https://learnui.design/tools/data-color-picker.html with primary as starting point
  ;; and secondary as endpoint
  ["#1292ee" "#8389f2" "#c37ce5" "#f36cc8" "#ff64a1" "#ff6c75" "#ff8247" "#ff9e0d"])

(defn get-graph-color
  "Returns a graph color, given an index."
  [index]
  (get graph-colors (mod index 8)))
