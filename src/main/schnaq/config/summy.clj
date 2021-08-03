(ns schnaq.config.summy
  (:require [ghostwheel.core :refer [>defn ?]]))

(def base-url
  "URL to our machine-learning service."
  (or (System/getenv "SUMMY_URL") "https://summy.schnaq.com"))

(>defn urls
  "Return the url to externally call machine learning functions."
  [key]
  [keyword? :ret (? string?)]
  (let [urls {:summary/bart "summary/bart"
              :summary/t5 "summary/t5"}
        url (get urls key)]
    (when url
      (format "%s/%s" base-url url))))

(def app-code "***REMOVED***")
