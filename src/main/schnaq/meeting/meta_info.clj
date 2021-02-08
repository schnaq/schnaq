(ns schnaq.meeting.meta-info
  (:require [ring.util.http-response :refer [ok created bad-request unauthorized]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.validator :as validator]))


(defn- discussion-meta-info [share-hash]
  (let [all-starting (discussion-db/starting-statements share-hash)
        all-arguments (discussion-db/all-arguments-for-discussion share-hash)
        total-count (+ (count all-starting) (count all-arguments))
        authors-starting (set (map #(:statement/author %) all-starting))
        authors-arguments (set (map #(:argument/author %) all-arguments))
        authors (set (concat authors-starting authors-arguments))]
    (count all-arguments)
    {:statements-num total-count :authors authors}))

(defn get-discussion-meta-info
  "Returns a meeting, identified by its share-hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (if (validator/valid-discussion? hash)
      (ok (discussion-meta-info hash))
      (validator/deny-access))))
