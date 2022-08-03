(ns schnaq.interface.components.lexical.nodes.excalidraw-image
  (:require ["@excalidraw/excalidraw" :refer [exportToSvg]]
            [oops.core :refer [ocall oget]]
            [promesa.core :as p]
            [reagent.core :as r]))

(defn- remove-external-excalidraw-fonts
  "Remove fonts from excalidraw.com, we import them manually from our servers."
  [svg]
  (let [style-tag (oget svg [:?firstElementChild :?firstElementChild])
        view-box (.getAttribute svg "viewBox")]
    (when view-box
      (let [view-box-dimensions (.split view-box " ")]
        (ocall svg "setAttribute" "width" (get view-box-dimensions 2))
        (ocall svg "setAttribute" "width" (get view-box-dimensions 3))))
    (when (and style-tag (= (oget style-tag :tagName) "style"))
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
