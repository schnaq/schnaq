(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [ghostwheel.core :refer [>defn]]
    [meetly.config :as config]
    [meetly.meeting.models :as models]
    [dialog.discussion.database :as dialog])
  (:import (java.util Date)))

(defonce ^:private datomic-client
  (d/client config/datomic))

(defn- new-connection
  "Connects to the database and returns a connection."
  []
  (d/connect datomic-client {:db-name config/db-name}))

(defn- transact
  "Shorthand for transaction."
  [data]
  (d/transact (new-connection) {:tx-data data}))

(defn- create-discussion-schema
  "Creates the schema for discussions inside the database."
  [connection]
  (d/transact connection {:tx-data models/datomic-schema}))

(defn- create-database-from-config!
  "Re-create a database based on the config-file."
  []
  (d/create-database
    datomic-client
    {:db-name config/db-name}))

(defn delete-database-from-config!
  []
  (d/delete-database
    datomic-client
    {:db-name config/db-name}))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj"
  []
  (when-not (= :peer-server (-> config/datomic :server-type))
    (create-database-from-config!))
  (create-discussion-schema (new-connection)))

(defn init-and-seed!
  "Initializing the datomic database and feeding it with test-data for the
  dialog.core."
  []
  (init!)
  (dialog/init! {:datomic config/datomic
                 :name config/db-name})
  (dialog/load-testdata!))

;; ##### Input functions #####
(defn now [] (Date.))

(defn add-meeting
  "Adds a meeting to the database. Returns the id of the newly added meeting."
  [meeting]
  (get-in
    (transact [(assoc meeting :db/id "newly-added-meeting")])
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
  "Add an agenda to the database.
  A discussion is automatically created for the agenda-point."
  [title description meeting-id]
  (transact [{:agenda/title title
              :agenda/description description
              :agenda/meeting meeting-id
              :agenda/discussion-id
              {:db/id "temp-id"
               :discussion/title title
               :discussion/description description
               :discussion/states [:discussion.state/open]
               :discussion/starting-arguments []}}]))

(def ^:private agenda-pattern
  [:db/id
   :agenda/title
   :agenda/description
   :agenda/meeting
   :agenda/discussion-id])

(defn agendas-by-meeting-hash
  "Return all agendas belonging to a certain meeting. Ready for the wire."
  [hash]
  (map first
       (d/q
         '[:find (pull ?agendas agenda-pattern)
           :in $ ?hash agenda-pattern
           :where [?agendas :agenda/meeting ?meeting]
           [?meeting :meeting/share-hash ?hash]]
         (d/db (new-connection)) hash agenda-pattern)))

(defn agenda-by-discussion-id
  "Returns an agenda which has the corresponding `discussion-id`."
  [discussion-id]
  (ffirst
    (d/q
      '[:find (pull ?agenda agenda-pattern)
        :in $ ?discussion-id agenda-pattern
        :where [?agenda :agenda/discussion-id ?discussion-id]]
      (d/db (new-connection)) discussion-id agenda-pattern)))

(>defn agenda-by-meeting-hash-and-discussion-id
  "Returns an agenda which fits to the provided meeting. So, we can directly
  verify that the agenda belongs to the issue."
  [meeting-hash discussion-id]
  [:meeting/share-hash :agenda/discussion-id
   :ret ::models/agenda]
  (ffirst
    (d/q
      '[:find (pull ?agenda agenda-pattern)
        :in $ ?meeting-hash ?discussion-id agenda-pattern
        :where [?meeting :meeting/share-hash ?meeting-hash]
        [?agenda :agenda/meeting ?meeting]
        [?agenda :agenda/discussion-id ?discussion-id]]
      (d/db (new-connection)) meeting-hash discussion-id agenda-pattern)))

(defn- author-exists?
  "Returns whether a certain author with `nickname` already exists in the db."
  [nickname]
  (seq
    (d/q
      '[:find ?lower-name
        :in $ ?author-name
        :where [_ :author/nickname ?original-nickname]
        [(.toLowerCase ^String ?original-nickname) ?lower-name]
        [(= ?lower-name ?author-name)]]
      (d/db (new-connection)) (.toLowerCase ^String nickname))))

(defn add-author
  "Add a new author to the database."
  [nickname]
  (transact [{:author/nickname nickname}]))

(defn add-author-if-not-exists
  "Adds an author if they do not exist yet."
  [nickname]
  (when-not (author-exists? nickname)
    (add-author nickname)))

(comment
  (init!)
  (add-meeting {:title "Test 1"
                :description "Jour Fixé des Hasses"
                :start-date (now)
                :end-date (now)
                :share-hash "897aasdha-12839hd-123dfa"})
  (all-meetings)
  (add-agenda-point "wegi" "Test-Beschreibung, wichtig!" 17592186045480)
  (meeting-by-hash "c58ec11d-46ff-489b-a7d3-9b466de497f0")
  (agendas-by-meeting-hash "897aasdha-12839hd-123dfa")
  (agenda-by-discussion-id "8f57fd87-ab35-4a50-99fb-73af3e07d4b5")
  (add-author "Shredder")
  (author-exists? "shredder")
  (add-author-if-not-exists "Shredder")
  :end)
