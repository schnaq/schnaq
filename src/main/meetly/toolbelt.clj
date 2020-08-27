(ns meetly.toolbelt
  "Utility functions supporting the backend."
  (:require [ghostwheel.core :refer [>defn]])
  (:import (java.io File)
           (org.joda.time DateTime)))

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
  (.toDate (.minusDays (DateTime.) days)))