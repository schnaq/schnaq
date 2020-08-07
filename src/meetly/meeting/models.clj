(ns meetly.meeting.models
  (:require [clojure.spec.alpha :as s]))

(def datomic-schema
  [{:db/ident :meeting/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The Title for the Meeting - does not have to be unique"}
   {:db/ident :meeting/start-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time the meetings officially starts at."}
   {:db/ident :meeting/end-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time after which participation is not possible anymore."}
   {:db/ident :meeting/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A description of the meetings purpose."}
   {:db/ident :meeting/share-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A hash that grants access to the discussion"}
   {:db/ident :meeting/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of a meeting."}
   ;; Agenda-Point
   {:db/ident :agenda/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The short title of the Agenda-Point"}
   {:db/ident :agenda/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A description of the point"}
   {:db/ident :agenda/meeting
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The meeting the agenda belongs to"}
   {:db/ident :agenda/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "An id belonging to the (foreign) discussion represented by this agenda"}
   ;; User
   {:db/ident :user/core-author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The author of dialog.core that corresponds to this user."}
   {:db/ident :user/upvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All upvotes the user gave."}
   {:db/ident :user/downvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All downvotes the user gave."}
   ;; Feedback
   {:db/ident :feedback/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The feedback description."}])

;; Common
(s/def ::entity-reference (s/or :transacted number? :temporary any?))

;; Meeting
(s/def :meeting/title string?)
(s/def :meeting/description string?)
(s/def :meeting/share-hash string?)
(s/def :meeting/start-date inst?)
(s/def :meeting/end-date inst?)
(s/def :meeting/author (s/or :reference ::entity-reference
                             :author :dialog.discussion.models/author))
(s/def ::meeting (s/keys :req [:meeting/title :meeting/author
                               :meeting/share-hash
                               :meeting/start-date :meeting/end-date]
                         :opt [:meeting/description]))

;; Agenda
(s/def :agenda/title string?)
(s/def :agenda/description string?)
(s/def :agenda/meeting ::entity-reference)
(s/def :agenda/discussion ::entity-reference)
(s/def ::agenda (s/keys :req [:agenda/title :agenda/meeting :agenda/discussion]
                        :opt [:agenda/description]))

(s/def :author/nickname string?)

;; Feedback
(s/def :feedback/description string?)
(s/def ::feedback (s/keys :req [:feedback/description]))