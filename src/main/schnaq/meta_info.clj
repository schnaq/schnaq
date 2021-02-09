(ns schnaq.meta-info
  (:require [schnaq.database.discussion :as discussion-db]))


(defn discussion-meta-info
  "Returns a hashmap of meta infos {:all-statements int, :authors '()}"
  [share-hash]
  (let [all-statements (discussion-db/all-statements share-hash)
        total-count (count all-statements)
        authors (distinct (map #(:statement/author %) all-statements))]
    {:all-statements total-count :authors authors}))