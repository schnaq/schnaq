(ns meetly.meeting.processors
  (:require [clojure.walk :as walk]
            [meetly.meeting.database :as db]
            [meetly.discussion :as discussion]
            [ghostwheel.core :refer [>defn]])
  (:import (clojure.lang PersistentArrayMap)))

(>defn with-votes
  "Enrich every statement map with its vote-counts."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % :statement/content))
       (assoc % :meta/upvotes (db/upvotes-for-statement (:db/id %))
                :meta/downvotes (db/downvotes-for-statement (:db/id %)))
       %)
    data))

(>defn with-sub-discussion-information
  "Enrich every statement map with its vote-counts."
  [data all-arguments]
  [any? sequential? :ret any?]
  (walk/postwalk
    #(if (and (= PersistentArrayMap (type %)) (contains? % :statement/content))
       (assoc % :meta/sub-discussion-info (discussion/sub-discussion-information (:db/id %) all-arguments))
       %)
    data))

(>defn with-canonical-usernames
  "Enriches a step-vector with canonical :user/nickname."
  [[step args]]
  [vector? :ret vector?]
  (let [username (:user/nickname args)
        new-args (assoc args :user/nickname (db/canonical-username username))]
    [step new-args]))