(ns schnaq.interface.utils.routing
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]))

(>defn prefix-route-name-locale
  "Prefixes a reitit-route name with a locale."
  [route-name prefix]
  [keyword? keyword? :ret keyword?]
  (keyword (str (name prefix) "." (namespace route-name)) (name route-name)))
