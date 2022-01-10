(ns schnaq.interface.utils.js-wrapper
  (:require ["jquery" :as jquery]
            [com.fulcrologic.guardrails.core :refer [>defn]]
            [goog.dom :as gdom]
            [goog.dom.dataset :as dataset]))

(>defn $
  "The jquery-lookup syntax."
  [lookup]
  [string? :ret any?]
  (jquery lookup))

(defn prop
  "Wrap the .prop function of jQuery."
  ([element prop]
   (.prop element prop))
  ([element prop value]
   (.prop element prop value)))

(>defn prevent-default
  "Wraps the js `<Event>.preventDefault` method to prevent Cursive warnings."
  [event]
  [any? :ret nil?]
  (.preventDefault event))

(>defn stop-propagation
  "Wraps the js `<Event>.stopPrapagation` method to prevent Cursive warnings."
  [event]
  [any? :ret nil?]
  (.stopPropagation event))

(>defn replace-url
  "Replaces the current URL in the users window and acts as a redirect."
  [new-url]
  [string? :ret nil?]
  (.replace (.-location js/window) new-url))

(defn show-js-klaro
  "Trigger klaro popup menu."
  []
  #(.show js/klaro))

(defn ctrl-press
  "Check for a ctrl + `keyCode` combination in `event`."
  [event keyCode]
  (and (.-ctrlKey event) (= keyCode (.-keyCode event))))

(defn browser-language
  "Returns the user's browser language"
  []
  (or (.-language js/navigator)
      (.-userLanguage js/navigator)))

(defn scroll-to-id
  [id]
  (let [clean-id (if (= \# (first id)) (subs id 1) id)
        element (.getElementById js/document clean-id)
        state (.-readyState js/document)]
    (when (and element (= state "complete"))
      (.scrollIntoView element))))

(def get-date-year
  "Get the current year"
  (.getFullYear (js/Date.)))

(defn number-trunc
  "Truncate a number"
  [number]
  (Math/trunc number))

(defn data-attribute
  "Reads a dataset attribute from some element by id."
  [element-id data-key]
  (-> element-id
      gdom/getElement
      (dataset/get data-key)))

(def document-body js/document.body)

(defn clear-input
  "Clears an input field."
  [id]
  (when-let [element (js/document.getElementById id)]
    (set! (.-value element) "")))
