(ns schnaq.meeting.processors
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn]]
            [schnaq.discussion :as discussion]
            [schnaq.meeting.database :as db])
  (:import (clojure.lang PersistentArrayMap)))

(>defn with-votes
  "Enrich every statement map with its vote-counts."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (instance? PersistentArrayMap %) (contains? % :statement/content))
       (assoc % :meta/upvotes (db/upvotes-for-statement (:db/id %))
                :meta/downvotes (db/downvotes-for-statement (:db/id %)))
       %)
    data))

(>defn with-sub-discussion-information
  "Enrich every statement map with its vote-counts."
  [data all-arguments]
  [any? sequential? :ret any?]
  (walk/postwalk
    #(if (and (instance? PersistentArrayMap %) (contains? % :statement/content))
       (assoc % :meta/sub-discussion-info (discussion/sub-discussion-information (:db/id %) all-arguments))
       %)
    data))

(>defn with-canonical-usernames
  "Enriches a step-vector with canonical :user/nickname."
  [[step args] current-nickname]
  [(s/tuple keyword? coll?) :author/nickname :ret (s/tuple keyword? coll?)]
  (let [new-args (assoc args :user/nickname (db/canonical-username current-nickname))]
    [step new-args]))