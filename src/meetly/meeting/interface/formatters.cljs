(ns meetly.meeting.interface.formatters
  (:require [clojure.walk :as walk]))


(defn- prepare-payload
  "Converts the Keywords to namespaced strings, before the clj->js function in the
  format of `http-xhrio / ajax-cljs` can strip it."
  [payload]
  (walk/postwalk
    #(if (= Keyword (type %))
       (str (symbol %))
       %)
    payload))

(defn- write-json-native [data]
  (.stringify js/JSON (clj->js (prepare-payload data))))

(defn- make-json-request-format [write-json]
  (fn json-request-format []
    {:write write-json
     :content-type "application/json"}))

(def namespaced-json-request-format
  (make-json-request-format write-json-native))