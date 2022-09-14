(ns schnaq.interface.utils.file-download
  (:require [oops.core :refer [oset!]]))

(defn file-blob [data mimetype]
  (js/Blob. [data] {"type" mimetype}))

(defn link-for-blob [blob filename]
  (doto (.createElement js/document "a")
    (set! -download filename)
    (set! -href (.createObjectURL js/URL blob))))

(defn click-and-remove-link [link]
  (let [click-remove-callback
        (fn []
          (.dispatchEvent link (js/MouseEvent. "click"))
          (.removeChild (.-body js/document) link))]
    (.requestAnimationFrame js/window click-remove-callback)))

(defn add-link [link]
  (.appendChild (.-body js/document) link))

(defn download-data [data filename mimetype]
  (-> data
      (file-blob mimetype)
      (link-for-blob filename)
      add-link
      click-and-remove-link))

(defn export-data [data]
  (download-data data "exported-discussion.txt" "text/plain"))

(defn- trigger-download
  "Create an anchor, set it's href and click it."
  [uri filename]
  (let [a (.createElement js/document "a")]
    (oset! a :download filename)
    (oset! a :href uri)
    (oset! a :target :_blank)
    (.click a)))

(defn download-svg-node
  "Download an svg DOM element."
  [svg filename]
  (let [data (.serializeToString (new js/XMLSerializer) svg)
        svgBlob (new js/Blob #js [data] #js {:type "image/svg+xml;charset=utf-8"})
        url (.createObjectURL js/URL svgBlob)]
    (trigger-download url filename)))
