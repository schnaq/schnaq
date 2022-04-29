(ns schnaq.shared-toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [=> >defn]])
  #?(:clj (:import (java.lang Character))))

(>defn slugify
  "Make a slug from a string. For example:
  `(slugify \"This is sparta\") => this-is-sparta`"
  [string]
  [string? => string?]
  (let [reduced-string (-> string
                           (string/replace #"/|\." " ")
                           string/trim
                           (string/split #"\s"))
        tokens (map string/lower-case reduced-string)]
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

(defn- alphanumeric?
  "Checks whether some char is a Letter or a Digit."
  [char-to-test]
  #?(:clj (or
           (Character/isLetter ^char char-to-test)
           (Character/isDigit ^char char-to-test))
     :cljs (.test (new js/RegExp "^[a-z0-9]+$") char-to-test)))

(defn tokenize-string
  "Tokenizes a string into single tokens for the purpose of searching."
  [content]
  (->> (string/split (string/lower-case content) #"\s")
       (remove string/blank?)
       ;; Remove punctuation when generating token
       (map #(cond
               (not (alphanumeric? (first %))) (subs % 1)
               (not (alphanumeric? (last %))) (subs % 0 (dec (count %)))
               :else %))))

(def select-values (comp vals select-keys))
