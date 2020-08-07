(ns meetly.meeting.database
  (:require
    [datomic.client.api :as d]
    [ghostwheel.core :refer [>defn >defn- ?]]
    [meetly.config :as config]
    [meetly.meeting.models :as models]
    [dialog.discussion.database :as dialog]
    [clojure.spec.alpha :as s]
    [clojure.string :as str])
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

(>defn delete-database!
  "Delete a database by its name."
  [database-name]
  [string? :ret boolean?]
  (d/delete-database
    datomic-client
    {:db-name database-name}))

(defn delete-database-from-config!
  "Deletes the pre-defined database from the configuration-file."
  []
  (delete-database! config/db-name))

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


;; -----------------------------------------------------------------------------
;; Pull Patterns

(def ^:private user-pattern
  "Pull a user based on these attributes."
  [:db/id
   :author/nickname])

(def ^:private meeting-pattern
  "Pull a meetly based on these attributes."
  [:db/id
   :meeting/title
   :meeting/start-date
   :meeting/end-date
   :meeting/description
   :meeting/share-hash
   {:meeting/author user-pattern}])


;; ##### Input functions #####
(defn now [] (Date.))

(>defn- clean-db-vals
  "Removes all entries from a map that have a value of nil or empty string."
  [data]
  [associative? :ret associative?]
  (into {} (filter #(not (or (nil? (second %))
                             (str/blank? (second %))))
                   data)))

(>defn add-meeting
  "Adds a meeting to the database. Returns the id of the newly added meeting.
  Automatically cleans input."
  [meeting]
  [associative? :ret int?]
  (let [clean-meeting (clean-db-vals meeting)]
    (when (s/valid? ::models/meeting clean-meeting)
      (get-in
        (transact [(assoc clean-meeting :db/id "newly-added-meeting")])
        [:tempids "newly-added-meeting"]))))

(defn all-meetings
  "Shows all meetings currently in the db."
  []
  (d/q
    '[:find (pull ?meetings meeting-pattern)
      :in $ meeting-pattern
      :where [?meetings :meeting/title _]]
    (d/db (new-connection)) meeting-pattern))

(defn meeting-by-hash
  "Returns the meeting corresponding to the share hash."
  [hash]
  (ffirst
    (d/q
      '[:find (pull ?meeting [*])
        :in $ ?hash
        :where [?meeting :meeting/share-hash ?hash]]
      (d/db (new-connection)) hash)))

(>defn add-agenda-point
  "Add an agenda to the database.
  A discussion is automatically created for the agenda-point.
  Returns the discussion-id of the newly created discussion."
  [title description meeting-id]
  [:agenda/title (? string?) int? :ret int?]
  (when (and (s/valid? :agenda/title title)
             (s/valid? int? meeting-id))
    (let [raw-agenda {:agenda/title title
                      :agenda/meeting meeting-id
                      :agenda/discussion
                      {:db/id "temp-id"
                       :discussion/title title
                       :discussion/states [:discussion.state/open]
                       :discussion/starting-arguments []}}
          agenda (if (and description (s/valid? :agenda/description description))
                   (merge-with merge
                               raw-agenda
                               {:agenda/description description
                                :agenda/discussion {:discussion/description description}})
                   raw-agenda)]
      (get-in
        (transact [agenda])
        [:tempids "temp-id"]))))

(def ^:private agenda-pattern
  [:db/id
   :agenda/title
   :agenda/description
   :agenda/meeting
   :agenda/discussion])

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
        :where [?agenda :agenda/discussion ?discussion-id]]
      (d/db (new-connection)) discussion-id agenda-pattern)))

(>defn agenda-by-meeting-hash-and-discussion-id
  "Returns an agenda which fits to the provided meeting. So, we can directly
  verify that the agenda belongs to the issue."
  [meeting-hash discussion-id]
  [:meeting/share-hash :agenda/discussion
   :ret ::models/agenda]
  (ffirst
    (d/q
      '[:find (pull ?agenda agenda-pattern)
        :in $ ?meeting-hash ?discussion-id agenda-pattern
        :where [?meeting :meeting/share-hash ?meeting-hash]
        [?agenda :agenda/meeting ?meeting]
        [?agenda :agenda/discussion ?discussion-id]]
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

(>defn add-user
  "Add a new user / author to the database."
  [nickname]
  [string? :ret int?]
  (when (s/valid? :author/nickname nickname)
    (get-in
      (transact [{:db/id "temp-user"
                  :user/core-author
                  {:db/id (format "id-%s" nickname)
                   :author/nickname nickname}}])
      [:tempids "temp-user"])))

(>defn add-user-if-not-exists
  "Adds an author if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:author/nickname :ret int?]
  (if-let [author-id (author-id-by-nickname nickname)]
    author-id
    (add-user nickname)))

(>defn user-by-nickname
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
  (count
    (d/q
      '[:find ?user
        :in $ ?statement
        :where [?user :user/upvotes ?statement]]
      (d/db (new-connection)) statement-id)))

(>defn downvotes-for-statement
  "Returns the number of downvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (count
    (d/q
      '[:find ?user
        :in $ ?statement
        :where [?user :user/downvotes ?statement]]
      (d/db (new-connection)) statement-id)))

(>defn remove-upvote!
  "Removes an upvote of a user."
  [statement-id user-nickname]
  [number? string? :ret associative?]
  (when-let [user (user-by-nickname user-nickname)]
    (transact [[:db/retract user :user/upvotes statement-id]])))

(>defn remove-downvote!
  "Removes a downvote of a user."
  [statement-id user-nickname]
  [number? string? :ret associative?]
  (when-let [user (user-by-nickname user-nickname)]
    (transact [[:db/retract user :user/downvotes statement-id]])))

(>defn- generic-reaction-check
  "Checks whether a user already made some reaction."
  [statement-id user-nickname field-name]
  [number? string? keyword? :ret (? number?)]
  (ffirst
    (d/q
      '[:find ?user
        :in $ ?statement ?nickname ?field-name
        :where [?author :author/nickname ?nickname]
        [?user :user/core-author ?author]
        [?user ?field-name ?statement]]
      (d/db (new-connection)) statement-id user-nickname field-name)))

(>defn did-user-upvote-statement
  "Check whether a user already upvoted a statement."
  [statement-id user-nickname]
  [number? string? :ret (? number?)]
  (generic-reaction-check statement-id user-nickname :user/upvotes))

(>defn did-user-downvote-statement
  "Check whether a user already downvoted a statement."
  [statement-id user-nickname]
  [number? string? :ret (? number?)]
  (generic-reaction-check statement-id user-nickname :user/downvotes))

(>defn check-valid-statement-id-and-meeting
  "Checks whether the statement-id matches the meeting-hash."
  [statement-id meeting-hash]
  [number? string? :ret (? number?)]
  (ffirst
    (d/q
      '[:find ?meeting
        :in $ ?statement ?hash
        :where (or [?argument :argument/premises ?statement]
                   [?argument :argument/conclusion ?statement])
        [?argument :argument/discussions ?discussion]
        [?agenda :agenda/discussion ?discussion]
        [?agenda :agenda/meeting ?meeting]
        [?meeting :meeting/share-hash ?hash]]
      (d/db (new-connection)) statement-id meeting-hash)))
