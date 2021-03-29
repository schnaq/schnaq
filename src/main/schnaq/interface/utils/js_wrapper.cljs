(ns schnaq.interface.utils.js-wrapper
  (:require ["jquery" :as jquery]
            [ghostwheel.core :refer [>defn]]))


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

(defn popover
  ([selector]
   (popover selector "enable"))
  ([selector option]
   (.popover (jquery selector) option)))

(>defn replace-url
  "Replaces the current URL in the users window and acts as a redirect."
  [new-url]
  [string? :ret nil?]
  (.replace (.-location js/window) new-url))

(defn show-js-klaro []
  #(.show js/klaro))