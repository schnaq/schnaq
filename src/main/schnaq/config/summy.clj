(ns schnaq.config.summy
  (:require [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [config.core :refer [env]]))

(def base-url
  "URL to our machine-learning service."
  (:summy-url env))

(>defn urls
  "Return the url to externally call machine learning functions."
  [key]
  [keyword? :ret (? string?)]
  (let [urls {:summary/bart "summary/bart"
              :summary/t5 "summary/t5"}
        url (get urls key)]
    (when url
      (format "%s/%s" base-url url))))

(def app-code (:app-code env))
