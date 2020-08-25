(ns meetly.interface.utils.js-wrapper)


(defn prevent-default [event]
  (.preventDefault event))

(defn stop-propagation [event]
  (.stopPropagation event))