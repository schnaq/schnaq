(ns schnaq.meta-info
  (:require [schnaq.database.discussion :as discussion-db]))


(defn discussion-meta-info
  "Returns a hashmap of meta infos {:all-statements int, :authors '()}"
  [share-hash author]
  (let [all-statements (discussion-db/all-statements share-hash)
        total-count (count all-statements)
        authors (distinct (conj (map #(-> % :statement/author :user/nickname) all-statements)
                                (:user/nickname author)))]
    {:all-statements total-count :authors authors}))