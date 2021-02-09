(ns schnaq.meeting.processors
  (:require [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.discussion :as discussion]
            [schnaq.meeting.database :as db]
            [schnaq.meeting.meta-info :as meta-info]
            [schnaq.meeting.specs :as specs])
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
       (assoc % :statement/content config/deleted-statement-text)
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

(>defn add-meta-info-to-schnaq
  "Enrich a schnaq its meta-infos."
  [schnaq]
  [::specs/discussion :ret ::specs/discussion]
  (let [share-hash (:discussion/share-hash schnaq)
        meta-info (meta-info/discussion-meta-info share-hash)]
    (assoc-in schnaq [:meta-info] meta-info)))
