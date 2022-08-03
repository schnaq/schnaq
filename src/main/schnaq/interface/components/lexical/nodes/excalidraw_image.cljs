(ns schnaq.interface.components.lexical.nodes.excalidraw-image
  (:require ["@excalidraw/excalidraw" :refer [exportToSvg]]
            [oops.core :refer [ocall oget]]
            [promesa.core :as p]
            [reagent.core :as r]))

(defn- remove-external-excalidraw-fonts
  "Remove fonts from excalidraw.com, we import them manually from our servers."
  [svg]
  (when-let [style-tag (oget svg [:?firstElementChild :?firstElementChild])]
    (when (= (oget style-tag :tagName) "style")
      (ocall style-tag "remove"))))

(defn ExcalidrawImage [_props]
  (let [drawing (r/atom nil)]
    (fn [props]
      (let [{:keys [elements imageContainerRef rootClassName]} props]
        (when-not @drawing
          (p/let [svg (exportToSvg #js {:elements elements :files nil})]
            (remove-external-excalidraw-fonts svg)
            (ocall svg "setAttribute" "width" "100%")
            (ocall svg "setAttribute" "height" "100%")
            (ocall svg "setAttribute" "display" "block")
            (reset! drawing svg)))
        (when @drawing
          [:div {:ref imageContainerRef
                 :className rootClassName
                 :dangerouslySetInnerHTML
                 {:__html (oget @drawing :?outerHTML)}}])))))
