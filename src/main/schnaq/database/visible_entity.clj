(ns schnaq.database.visible-entity
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [transact query]]
            [taoensso.timbre :as log]))

(>defn add-entity!
  "Adds a new starting-statement and returns the newly created id."
  [share-hash visible-id]
  [:discussion/share-hash :db/id :ret map?]
  (log/info "Adding visible element:" visible-id)
  (let [discussion-id (:db/id (discussion-db/discussion-by-share-hash share-hash))]
    @(transact [[:db/add discussion-id :discussion.visible/entities visible-id]])))

(>defn retract-entity!
  [share-hash visible-id]
  [:discussion/share-hash :db/id :ret map?]
  (let [discussion-id (:db/id (discussion-db/discussion-by-share-hash share-hash))]
    @(transact [[:db/retract discussion-id :discussion.visible/entities visible-id]])))

(>defn get-entities
  "Get the visible entities for a discussion."
  [share-hash]
  [:discussion/share-hash :ret vector?]
  (mapv :db/ident
        (query
         '[:find [(pull ?visible-entities [:db/ident]) ...]
           :in $ ?share-hash
           :where [?discussion :discussion/share-hash ?share-hash]
           [?discussion :discussion.visible/entities ?visible-entities]]
         share-hash)))
