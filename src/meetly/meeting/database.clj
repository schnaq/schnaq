(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [meetly.config :as config]
    [meetly.meeting.dialog-connector :as dialogs]
    [meetly.meeting.models :as models])
  (:import (java.util Date)))


(defonce datomic-client
         (d/client config/datomic))

(defn new-connection
  "Connects to the database and returns a connection."
  []
  (d/connect datomic-client {:db-name config/db-name}))

(defn transact
  "Shorthand for transaction."
  [data]
  (d/transact (new-connection) {:tx-data data}))

(defn- create-discussion-schema
  "Creates the schema for discussions inside the database."
  [connection]
  (d/transact connection {:tx-data models/datomic-schema}))

(defn init
  "Initialization function, which does everything needed at a fresh app-install."
  []
  (create-discussion-schema (new-connection)))

;; ##### Input functions #####
(defn now [] (Date.))

(defn add-meeting
  "Adds a meeting to the database. Returns the id of the newly added meeting."
  [{:keys [title description end-date start-date share-hash]}]
  (get-in
    (transact [{:db/id "newly-added-meeting"
                :meeting/title title
                :meeting/description description
                :meeting/end-date end-date
                :meeting/start-date start-date
                :meeting/share-hash share-hash}])
    [:tempids "newly-added-meeting"]))

(defn all-meetings
  "Shows all meetings currently in the db."
  []
  (d/q
    '[:find (pull ?meetings [*])
      :where [?meetings :meeting/title _]]
    (d/db (new-connection))))

(defn meeting-by-hash
  "Returns the meeting corresponding to the share hash."
  [hash]
  (ffirst
    (d/q
      '[:find (pull ?meeting [*])
        :in $ ?hash
        :where [?meeting :meeting/share-hash ?hash]]
      (d/db (new-connection)) hash)))

(defn add-agenda-point
  "Add an agenda to the database."
  [title description meeting-id]
  (transact [{:agenda/title title
              :agenda/description description
              :agenda/meeting meeting-id
              :agenda/discussion-id
              (dialogs/create-discussion-for-agenda title description)}]))

(comment
  (init)
  (add-meeting {:title "Test 1"
                :description "Jour Fixé des Hasses"
                :start-date (now)
                :end-date (now)
                :share-hash "897aasdha-12839hd-123dfa"})
  (all-meetings)
  (add-agenda-point "Gründungsform" "UG oder doch GmbH?" 17592186045450)
  (meeting-by-hash "897aasdha-12839hd-123dfa")
  :end)
