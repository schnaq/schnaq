(ns meetly.toolbelt
  "Utility functions support the backend."
  (:import (java.io File)))

(defn create-storage-directory!
  "Locally creates a file to store datomic data."
  []
  (let [dir (File. ".datomic/dev-local/data")]
    (.mkdir dir)
    (.getAbsolutePath dir)))