(ns schnaq.meeting.processors
  (:require [clojure.walk :as walk]
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

(>defn hide-deleted-statement-content
  "For all statements, that have a deleted? flag, hide them."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (instance? PersistentArrayMap %) (contains? % :statement/content) (:statement/deleted? %))
       (assoc % :statement/content "[deleted]")
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
