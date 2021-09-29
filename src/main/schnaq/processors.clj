(ns schnaq.processors
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.meta-info :as meta-info])
  (:import (clojure.lang IEditableCollection)))


;; -----------------------------------------------------------------------------
;; Processing schnaqs

(>defn add-meta-info-to-schnaq
  "Enrich a schnaq with its meta-infos."
  [schnaq]
  [::specs/discussion :ret ::specs/discussion]
  (let [share-hash (:discussion/share-hash schnaq)
        author (:discussion/author schnaq)
        meta-info (meta-info/discussion-meta-info share-hash author)]
    (assoc schnaq :meta-info meta-info)))


;; -----------------------------------------------------------------------------
;; Processing statements

(>defn with-aggregated-votes
  "Anonymize the votes by just counting the numer of votes and adding whether the user has upvoted or not."
  [data user-id]
  [any? :db/id :ret any?]
  (walk/postwalk
    #(if (and (instance? IEditableCollection %)
              (or (contains? % :statement/downvotes) (contains? % :statement/upvotes)))
       (let [upvotes (map :db/id (:statement/upvotes %))
             downvotes (map :db/id (:statement/downvotes %))]
         (assoc % :statement/upvotes (count upvotes)
                  :statement/downvotes (count downvotes)
                  :meta/upvoted? (contains? (set upvotes) user-id)
                  :meta/downvoted? (contains? (set downvotes) user-id)))
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

(defn with-new-post-info
  "Add sub-discussion-info whether a user has seen this post already."
  [data share-hash user-identity]
  (if user-identity
    (let [known-statements (user-db/known-statement-ids user-identity share-hash)]
      (walk/postwalk
        (fn [statement]
          (if (and (s/valid? ::specs/statement statement)
                   (not= user-identity
                         (get-in statement [:statement/author :user.registered/keycloak-id])))
            (assoc statement :meta/new? (not (contains? known-statements (:db/id statement))))
            statement))
        data))
    data))

(defn with-sub-discussion-info
  "Add sub-discussion-info to valid statements, if necessary.
   Sub-Discussion-infos are number of sub-statements, authors, ..."
  [data]
  (walk/postwalk
    (fn [statement]
      (if (s/valid? ::specs/statement statement)
        (if-let [sub-discussions (get (discussion-db/child-node-info [(:db/id statement)]) (:db/id statement))]
          (assoc statement :meta/sub-discussion-info sub-discussions)
          statement)
        statement))
    data))
