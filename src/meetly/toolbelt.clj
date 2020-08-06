(ns meetly.toolbelt
  "Utility functions supporting the backend."
  (:require [ghostwheel.core :refer [>defn]]
            [clojure.spec.alpha :as s])
  (:import (java.io File)))

(>defn create-storage-directory!
  "Locally creates a file to store datomic data. Optionally takes a sub-path for
  folder-creation."
  ([]
   [:ret string?]
   (create-storage-directory! "data"))
  ([sub-path]
   [string? :ret string?]
   (let [dir (File. (format ".datomic/dev-local/%s" sub-path))]
     (.mkdir dir)
     (.getAbsolutePath dir))))

(>defn conforms?
  "Shortcut to get truth value instead of s/conform results."
  [spec data]
  [keyword? any? :ret boolean?]
  (not= :clojure.spec.alpha/invalid (s/conform spec data)))