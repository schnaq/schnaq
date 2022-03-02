(ns schnaq.shared-toolbelt
  (:require [clojure.string :as string]
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
