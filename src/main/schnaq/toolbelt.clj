(ns schnaq.toolbelt
  "Utility functions supporting the backend."
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [com.fulcrologic.guardrails.core :refer [>defn ?]])
  (:import (clojure.lang PersistentArrayMap)
           (java.time Instant LocalDateTime ZoneOffset)
           (java.time.temporal ChronoUnit TemporalUnit)))

(>defn now-minus-days
  "Returns an instant that represents the current date minus some days. Assumes systemDefault timezone."
  [days]
  [int? :ret inst?]
  (.minus (Instant/now) ^Long days ^TemporalUnit ChronoUnit/DAYS))

(>defn now-plus-days-instant
  "Adds a number of days to the current datetime and then converts that to an instant."
  [days]
  [integer? :ret inst?]
  (.toInstant (.plusDays (LocalDateTime/now) days) ZoneOffset/UTC))

(>defn pull-key-up
  "Finds any occurrence of a member of `key-name` in `coll`. Then replaced the corresponding
   value with the value of its key-name entry.
   E.g.
   ```
   (ident-map->value {:foo {:db/ident :bar}, :baz {:db/ident :oof}} :db/ident)
   => {:foo :bar, :baz :oof}

   (ident-map->value {:foo {:db/ident :bar}} :not-found)
   => {:foo {:db/ident :bar}}
   ```"
  ([coll]
   [(? coll?) :ret (? coll?)]
   (pull-key-up coll :db/ident))
  ([coll key-name]
   [(? coll?) keyword? :ret (? coll?)]
   (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % key-name))
       (key-name %)
       %)
    coll)))

(>defn ascending
  "Comparator, can be used to sort collections in an ascending way."
  [a b]
  [any? any? :ret number?]
  (compare b a))

(>defn build-allowed-origin
  "Build regular expressions, which define the allowed origins for API requests."
  [domain]
  [string? :ret any?]
  (when-let [[domain-name tld] (string/split domain #"\.")]
    (let [regex (if tld
                  "^((https?://)?(.*\\.)?(%s\\.%s))($|/.*$)"
                  "^((https?://)?(.*\\.)?(%s))($|/.*$)")]
      (re-pattern (format regex domain-name tld)))))

(def synonyms-german (atom {}))

;; Excellent macro copied from user kotarak: https://stackoverflow.com/a/1879961/552491
(defn try-times*
  "Executes thunk. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n thunk]
  (loop [n n]
    (if-let [result (try
                      [(thunk)]
                      (catch Exception e
                        (when (zero? n)
                          (throw e))))]
      (result 0)
      (recur (dec n)))))

(defmacro try-times
  "Executes body. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n & body]
  `(try-times* ~n (fn [] ~@body)))
