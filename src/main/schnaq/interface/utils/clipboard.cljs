(ns schnaq.interface.utils.clipboard
  (:require [ghostwheel.core :refer [>defn]]))

(>defn copy-to-clipboard!
  "Copies a string to the users clipboard."
  [value]
  [string? :ret any?]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) value)
    (.appendChild js/document.body el)
    (.select el)
    (js/document.execCommand "copy")
    (.removeChild js/document.body el)))