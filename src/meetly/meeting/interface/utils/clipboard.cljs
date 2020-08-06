(ns meetly.meeting.interface.utils.clipboard
  (:require [ghostwheel.core :refer [>defn]]))

(>defn copy-to-clipboard!
  "Copies a string to the users clipboard."
  [val]
  [string? :ret any?]
  (let [el (js/document.createElement "textarea")]
    (set! (.-value el) val)
    (.appendChild js/document.body el)
    (.select el)
    (js/document.execCommand "copy")
    (.removeChild js/document.body el)))