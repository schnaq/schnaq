(ns schnaq.interface.components.lexical.nodes.excalidraw-image
  (:require [oops.core :refer [oget]]
            [reagent.core :as r]
            [schnaq.interface.components.lexical.nodes.excalidraw-utils :refer [elements->svg]]))

(defn ExcalidrawImage [_props]
  (let [drawing (r/atom nil)]
    (fn [props]
      (let [{:keys [elements imageContainerRef rootClassName]} props]
        (when-not @drawing
          (elements->svg elements #(reset! drawing %)))
        (when @drawing
          [:div {:ref imageContainerRef
                 :className rootClassName
                 :dangerouslySetInnerHTML
                 {:__html (oget @drawing :?outerHTML)}}])))))
