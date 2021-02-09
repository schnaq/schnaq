(ns schnaq.meeting.meta-info
  (:require [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [ok]]
            [schnaq.database.discussion :as discussion-db]))


(defn discussion-meta-info
  "Returns a hashmap of meta infos {:all-statements int, :authors '()}"
  [share-hash]
  (let [all-statements (discussion-db/all-statements share-hash)
        total-count (count all-statements)
        authors (distinct (map #(:statement/author %) all-statements))]
    {:all-statements total-count :authors authors}))