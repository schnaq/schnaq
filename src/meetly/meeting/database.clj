(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [ghostwheel.core :refer [>defn >defn-]]
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

(>defn create-database!
  "Create a new database. Does not check whether there already is an existing
  database with the same name."
  [database-name]
  [string? :ret boolean?]
  (d/create-database
    datomic-client
    {:db-name database-name}))

(defn delete-database-from-config!
  []
  (d/delete-database
    datomic-client
    {:db-name config/db-name}))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init! {:datomic config/datomic
           :name config/db-name}))
  ([config]
   (when-not (= :peer-server (-> (:datomic config) :server-type))
     (create-database! (:name config)))
   (create-discussion-schema (new-connection))))

(defn init-and-seed!
  "Initializing the datomic database and feeding it with test-data for the
  dialog.core.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init-and-seed! {:datomic config/datomic
                    :name config/db-name}))
  ([config]
   (init! config)
   (dialog/init! config)
   (dialog/load-testdata!)))


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

(>defn- author-id-by-nickname
  "Returns an author-id by nickname."
  [nickname]
  [string? :ret number?]
  (ffirst
    (d/q
      '[:find ?author
        :in $ ?author-name
        :where [?author :author/nickname ?original-nickname]
        [(.toLowerCase ^String ?original-nickname) ?lower-name]
        [(= ?lower-name ?author-name)]]
      (d/db (new-connection)) (.toLowerCase ^String nickname))))

(defn add-user
  "Add a new user / author to the database."
  [nickname]
  (transact [{:user/core-author
              {:author/nickname nickname}}]))

(defn add-user-if-not-exists
  "Adds an author if they do not exist yet."
  [nickname]
  (when-not (author-id-by-nickname nickname)
    (add-user nickname)))

(>defn- user-by-nickname
  "Return the **meetly** user-id by nickname."
  [nickname]
  [string? :ret number?]
  (ffirst
    (d/q
      '[:find ?user
        :in $ ?nickname
        :where [?user :user/core-author ?author]
        [?author :author/nickname ?nickname]]
      (d/db (new-connection)) nickname)))

(>defn- vote-on-statement!
  "Up or Downvote a statement"
  [statement-id user-nickname vote-type]
  [number? string? keyword?
   :ret associative?]
  (let [user (user-by-nickname user-nickname)
        [add-field remove-field] (if (= vote-type :upvote)
                                   [:user/upvotes :user/downvotes]
                                   [:user/downvotes :user/upvotes])]
    (when user
      (transact [[:db/retract user remove-field statement-id]
                 [:db/add user add-field statement-id]]))))

(>defn upvote-statement!
  "Upvotes a statement. Takes a user-nickname and a statement-id. The user has to exist, otherwise
  nothing happens."
  [statement-id user-nickname]
  [number? string?
   :ret associative?]
  (vote-on-statement! statement-id user-nickname :upvote))

(>defn downvote-statement!
  "Downvotes a statement. Takes a user-nickname and a statement-id. The user has to exist, otherwise
  nothing happens."
  [statement-id user-nickname]
  [number? string?
   :ret associative?]
  (vote-on-statement! statement-id user-nickname :downvote))

(>defn upvotes-for-statement
  "Returns the number of upvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (d/q
    '[:find (count ?user)
      :in $ ?statement
      :where [?user :user/upvotes ?statement]]
    (d/db (new-connection)) statement-id))

(>defn downvotes-for-statement
  "Returns the number of downvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (d/q
    '[:find (count ?user)
      :in $ ?statement
      :where [?user :user/downvotes ?statement]]
    (d/db (new-connection)) statement-id))
