(ns schnaq.meeting.meta-info
  (:require [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [ok]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.validator :as validator]))


(defn- discussion-meta-info
  "Returns a hashmap of meta infos {:all-statements int, :authors #{}}"
  [share-hash]
  (let [all-starting (discussion-db/starting-statements share-hash)
        all-arguments (discussion-db/all-arguments-for-discussion share-hash)
        total-count (+ (count all-starting) (count all-arguments))
        authors-starting (set (map #(:statement/author %) all-starting))
        authors-arguments (set (map #(:argument/author %) all-arguments))
        authors (set (concat authors-starting authors-arguments))]
    (count all-arguments)
    {:all-statements total-count :authors authors}))

(defn- discussion-meta-infos
  "Returns a hashmap of share-hashes and meta infos"
  [share-hashes]
  (let [meta-fn (fn [hash] (when (validator/valid-discussion? hash)
                             {hash (discussion-meta-info hash)}))
        all-meta-info (map meta-fn share-hashes)]
    (into (hash-map) all-meta-info)))

(defn get-discussion-meta-info
  "Returns meta information of a schnaq by hash."
  [req]
  (let [hash (get-in req [:route-params :hash])]
    (if (validator/valid-discussion? hash)
      (ok (discussion-meta-info hash))
      (validator/deny-access))))

(>defn get-multiple-discussion-meta-infos
  "Returns a hashmap of meta information"
  [{:keys [params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hashes]} params
        meta-info-map (discussion-meta-infos share-hashes)]
    (ok meta-info-map)))