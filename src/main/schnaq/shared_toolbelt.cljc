(ns schnaq.shared-toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn =>]]))

(>defn slugify
  "Make a slug from a string. For example:
  `(slugify \"This is sparta\") => this-is-sparta`"
  [string]
  [string? => string?]
  (let [tokens (map #(string/lower-case %) (string/split string #"\s"))]
    (string/join "-" (take (count tokens) tokens))))

(>defn remove-nil-values-from-map
  "Removes all entries from a map that have a value of nil or empty string."
  [data]
  [associative? :ret associative?]
  (into {} (remove #(or (nil? (second %))
                        (when (string? (second %))
                          (string/blank? (second %))))
                   data)))

(>defn normalize
  "Normalize a collection of maps to a map with the key as its identity, e.g. the
  db/id, and the value is the original map."
  [ident coll]
  [keyword? (s/coll-of map?) => map?]
  (into {} (map (juxt ident identity) coll)))
