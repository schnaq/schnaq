(ns schnaq.interface.components.lexical.nodes.excalidraw-utils
  (:require ["@excalidraw/excalidraw" :refer [exportToSvg]]
            [oops.core :refer [ocall oget]]
            [promesa.core :as p]))

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

(defn- set-attributes-for-svg [svg]
  (ocall svg "setAttribute" "width" "100%")
  (ocall svg "setAttribute" "height" "100%")
  (ocall svg "setAttribute" "display" "block"))

(defn convert-elements-to-svg
  "Convert the excalidraw elements to an svg. This is a promise, so we take a
  callback function receiving the updated svg."
  [elements callback]
  (p/let [svg (exportToSvg #js {:elements elements :files nil})]
    (remove-external-excalidraw-fonts svg)
    (set-attributes-for-svg svg)
    (callback svg)))
