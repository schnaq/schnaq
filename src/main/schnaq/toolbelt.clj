(ns schnaq.toolbelt
  "Utility functions supporting the backend."
  (:require [ghostwheel.core :refer [>defn]]
            [clojure.walk :as walk])
  (:import (java.io File)
           (java.time ZonedDateTime)
           (clojure.lang PersistentArrayMap)))

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
  (.toInstant (.minusDays (ZonedDateTime/now) days)))

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
  [coll? keyword? :ret coll?]
  (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % key-name))
       (key-name %)
       %)
    coll))
