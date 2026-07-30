(ns schnaq.interface.utils.file-download
  (:require [clojure.string :as str]
            [oops.core :refer [oset!]]))

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

(defn- csv-escape
  "Escape a value per RFC 4180: wrap in quotes if it contains comma, quote,
   or newline; double internal quotes."
  [v]
  (let [s (str v)]
    (if (re-find #"[,\"\r\n]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn rows->csv
  "Convert rows (seq of seqs) to a CSV string. First row is the header."
  [rows]
  (->> rows
       (map (fn [row] (str/join "," (map csv-escape row))))
       (str/join "\n")))

(defn download-csv
  "Trigger CSV file download for the given rows."
  [rows filename]
  (download-data (rows->csv rows) filename "text/csv;charset=utf-8"))
