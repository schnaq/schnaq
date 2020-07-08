(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [meetly.config :as config]
    [meetly.meeting.models :as models]))


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
(defn now [] (java.util.Date.))

(defn add-meeting
  "Adds a meeting to the database"
  [{:keys [title description end-date start-date share-hash]}]
  (transact [{:meeting/title title
              :meeting/description description
              :meeting/end-date end-date
              :meeting/start-date start-date
              :meeting/share-hash share-hash}]))

;; TODO Continue here. Build out the query and wire it to the api
;; TODO on the other side at the front end query it periodically or at the start
;; TODO then show the updated list.
;; TODO afterwards write all new frontend-entries to the db
(defn all-meetings
  "Shows all meetings currently in the db."
  []
  (d/q))

(comment
  (add-meeting {:title "Test"
                :description "Jour Fixé der Liebe"
                :start-date (now)
                :end-date (now)
                :share-hash "897aha-12839hd-123dfa"})
  :end)
