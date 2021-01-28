(ns schnaq.toolbelt
  "Utility functions supporting the backend."
  (:require [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn ?]])

  (:import (clojure.lang PersistentArrayMap)
           (java.io File)
           (java.time Instant)
           (java.time.temporal ChronoUnit TemporalUnit)))

(>defn create-directory!
  "Creates a directory in the project's path. Returns the absolut path of the
  directory."
  [^String path]
  [string? :ret string?]
  (when-not (or (.startsWith path "/")
                (.startsWith path ".."))
    (let [dir (File. path)]
      (.mkdirs dir)
      (.getAbsolutePath dir))))

(>defn now-minus-days
  "Returns an instant that represents the current date minus some days. Assumes systemDefault timezone."
  [days]
  [int? :ret inst?]
  (.minus (Instant/now) ^Long days ^TemporalUnit ChronoUnit/DAYS))

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
  [coll key-name]
  [(? coll?) keyword? :ret (? coll?)]
  (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % key-name))
       (key-name %)
       %)
    coll))


(defn comp-compare
  "A compare that exhibits the exact opposite behaviour."
  [val1 val2]
  (case (compare val1 val2)
    0 0
    -1 1
    1 -1))