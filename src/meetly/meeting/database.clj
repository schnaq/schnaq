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

(defn- clean-agenda
  "Cleans the stubborn parts of an agenda."
  [agenda]
  (let [agenda-point (first agenda)
        id (get-in agenda-point [:agenda/meeting :db/id])]
    (-> agenda-point
        (assoc :meeting id)
        (dissoc :agenda/meeting))))

(defn agendas-by-meeting
  "Return all agendas belonging to a certain meeting. Ready for the wire."
  [meeting-id]
  (map clean-agenda
       (d/q
         '[:find (pull ?agendas [[:agenda/title :as :title]
                                 [:agenda/description :as :description]
                                 :agenda/meeting
                                 [:agenda/discussion-id :as :discussion-id]])
           :in $ ?meeting-id
           :where [?agendas :agenda/meeting ?meeting-id]]
         (d/db (new-connection)) meeting-id)))

(comment
  (init)
  (add-meeting {:title "Test 1"
                :description "Jour Fixé des Hasses"
                :start-date (now)
                :end-date (now)
                :share-hash "897aasdha-12839hd-123dfa"})
  (all-meetings)
  (add-agenda-point "nico" "ist der" 17592186045445)
  (meeting-by-hash "897aasdha-12839hd-123dfa")
  (agendas-by-meeting 17592186045445)
  :end)
