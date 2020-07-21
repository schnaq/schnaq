(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [meetly.config :as config]
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
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj"
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

(defn- clean-agenda
  "Cleans the stubborn parts of an agenda."
  [agenda]
  (let [agenda-point (first agenda)
        id (get-in agenda-point [:agenda/meeting :db/id])]
    (when agenda-point
      (-> agenda-point
          (assoc :meeting id)
          (dissoc :agenda/meeting)))))

(def ^:private agenda-pattern
  [[:agenda/title :as :title]
   [:agenda/description :as :description]
   :agenda/meeting
   [:agenda/discussion-id :as :discussion-id]])

(defn agendas-by-meeting-hash
  "Return all agendas belonging to a certain meeting. Ready for the wire."
  [hash]
  (map clean-agenda
       (d/q
         '[:find (pull ?agendas agenda-pattern)
           :in $ ?hash agenda-pattern
           :where [?agendas :agenda/meeting ?meeting]
           [?meeting :meeting/share-hash ?hash]]
         (d/db (new-connection)) hash agenda-pattern)))

(defn agenda-by-discussion-id
  "Returns an agenda which has the corresponding `discussion-id`."
  [discussion-id]
  (clean-agenda
    (first
      (d/q
        '[:find (pull ?agenda agenda-pattern)
          :in $ ?discussion-id agenda-pattern
          :where [?agenda :agenda/discussion-id ?discussion-id]]
        (d/db (new-connection)) discussion-id agenda-pattern))))

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
  (init)
  (add-meeting {:title "Test 1"
                :description "Jour Fixé des Hasses"
                :start-date (now)
                :end-date (now)
                :share-hash "897aasdha-12839hd-123dfa"})
  (all-meetings)
  (add-agenda-point "wegi" "Test-Beschreibung, wichtig!" 17592186045445)
  (meeting-by-hash "897aasdha-12839hd-123dfa")
  (agendas-by-meeting-hash "b1c9676f-3a5d-4c6d-b63b-6b18144efdd7")
  (agenda-by-discussion-id "8f57fd87-ab35-4a50-99fb-73af3e07d4b5")
  (add-author "Shredder")
  (author-exists? "shredder")
  (add-author-if-not-exists "Shredder")
  :end)
