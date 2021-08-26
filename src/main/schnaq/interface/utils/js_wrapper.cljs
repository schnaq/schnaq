(ns schnaq.interface.utils.js-wrapper
  (:require ["jquery" :as jquery]
            ["react-tippy" :refer [Tooltip]]
            [ghostwheel.core :refer [>defn]]
            [goog.dom :as gdom]
            [goog.dom.dataset :as dataset]
            [reagent.core :as r]))


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

(>defn add-listener
  "Adds an event-listener to one or more elements matching the `selector`. When the
  event fires, the `listener-fn` ist executed."
  [selector event-name listener-fn]
  [string? string? fn? :ret nil?]
  (.on (jquery selector) event-name listener-fn))

(>defn remove-listener
  "Remove an event-listener from one or more elements matching `selector`."
  [selector event-name]
  [string? string? :ret nil?]
  (.off (jquery selector) event-name))

(>defn element-index
  "Shows the index of the selected element inside of its parent element."
  [selector]
  [string? :ret int?]
  (.index (jquery selector)))

(defn tooltip
  ([selector]
   (tooltip selector "enable"))
  ([selector option]
   (.tooltip (jquery selector) option)))

(defn TippyHtml
  [component content options]
  [:> Tooltip
   (merge
     {:animation "scale"
      :arrow true
      :html (r/as-element component)
      :interactive true
      :offset 5
      :position "bottom"
      :theme "light"
      :trigger "click"}
     options)
   content])

(defn TippyText
  [title content options]
  [:> Tooltip
   (merge
     {:animation "scale"
      :arrow true
      :offset 5
      :position "bottom"
      :theme "light"
      :title title}
     options)
   content])

(>defn replace-url
  "Replaces the current URL in the users window and acts as a redirect."
  [new-url]
  [string? :ret nil?]
  (.replace (.-location js/window) new-url))

(defn show-js-klaro
  "Trigger klaro popup menu."
  []
  #(.show js/klaro))

(defn to-string
  "Converts something to string via js."
  [input]
  (.toString input))

(defn ctrl-press
  "Check for a ctrl + `keyCode` kombination in `event`."
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