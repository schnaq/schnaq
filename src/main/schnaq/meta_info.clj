(ns schnaq.meta-info
  (:require [schnaq.database.discussion :as discussion-db]
            [schnaq.user :as user]))

(defn discussion-meta-info
  "Returns a hashmap of meta infos {:all-statements int, :authors '()}"
  [share-hash author]
  (let [all-statements (discussion-db/all-statements share-hash)
        total-count (count all-statements)
        authors (distinct (conj (map #(user/statement-author %)
                                     all-statements)
                                (user/display-name author)))]
    {:all-statements total-count :authors authors}))