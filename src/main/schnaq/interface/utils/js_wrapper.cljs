(ns schnaq.interface.utils.js-wrapper
  (:require ["jquery" :as jquery]
            [ghostwheel.core :refer [>defn]]))


(>defn prevent-default
  "Wraps the js `<Event>.preventDefault` method to prevent cursive warnings."
  [event]
  [any? :ret nil?]
  (.preventDefault event))

(>defn stop-propagation
  "Wraps the js `<Event>.stopPrapagation` method to prevent cursive warnings."
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