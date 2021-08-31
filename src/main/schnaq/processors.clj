(ns schnaq.processors
  (:require [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.reaction :as reaction-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.meta-info :as meta-info])
  (:import (clojure.lang IEditableCollection)))

(>defn with-votes
  "Enrich every statement map with its vote-counts."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (instance? IEditableCollection %) (contains? % :statement/content))
       (assoc % :meta/upvotes (reaction-db/upvotes-for-statement (:db/id %))
                :meta/downvotes (reaction-db/downvotes-for-statement (:db/id %)))
       %)
    data))

(>defn hide-deleted-statement-content
  "For all statements, that have a deleted? flag, hide them."
  [data]
  [any? :ret any?]
  (walk/postwalk
    #(if (and (instance? IEditableCollection %) (contains? % :statement/content) (:statement/deleted? %))
       (assoc % :statement/content config/deleted-statement-text)
       %)
    data))

(>defn add-meta-info-to-schnaq
  "Enrich a schnaq with its meta-infos."
  [schnaq]
  [::specs/discussion :ret ::specs/discussion]
  (let [share-hash (:discussion/share-hash schnaq)
        author (:discussion/author schnaq)
        meta-info (meta-info/discussion-meta-info share-hash author)]
    (assoc schnaq :meta-info meta-info)))

(defn with-new-post-info
  "Add sub-discussion-info whether or not a user has seen this post already."
  [statements share-hash user-identity]
  (if user-identity
    (let [known-statements (user-db/known-statement-ids user-identity share-hash)]
      (map #(assoc % :meta/new (not (contains? known-statements (:db/id %)))) statements))
    statements))

(defn with-sub-discussion-info
  "Add sub-discussion-info, if necessary. Sub-Discussion-infos are number of
  sub-statements, authors, ..."
  [statements]
  (let [statement-ids (map :db/id statements)
        info-map (discussion-db/child-node-info statement-ids)]
    (map (fn [statement]
           (if-let [sub-discussions (get info-map (:db/id statement))]
             (assoc statement :meta/sub-discussion-info sub-discussions)
             statement))
         statements)))

(defn with-sub-discussion-info-on-schnaq
  "Same as `with-sub-discussion-info`, but enriches the starting statements in a
  schnaq."
  [schnaq]
  (assoc schnaq :discussion/starting-statements
                (with-sub-discussion-info (:discussion/starting-statements schnaq))))
