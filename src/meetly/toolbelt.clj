(ns meetly.toolbelt
  "Utility functions supporting the backend."
  (:require [ghostwheel.core :refer [>defn]])
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
