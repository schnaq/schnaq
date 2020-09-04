(ns schnaq.interface.utils.js-wrapper
  (:require ["jquery" :as jquery]))


(defn prevent-default [event]
  (.preventDefault event))

(defn stop-propagation [event]
  (.stopPropagation event))

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