(ns schnaq.meeting.database
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [datomic.client.api :as d]
            [dialog.discussion.database :as dialog]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.config :as config]
            [schnaq.meeting.models :as models]
            [schnaq.test-data :as test-data])
  (:import (java.util Date UUID)))

(def ^:private datomic-info
  (atom {:client nil
         :database-name nil}))

(>defn- reset-datomic-client!
  "Sets a new datomic client for transactions."
  [datomic-config]
  [map? :ret any?]
  (swap! datomic-info assoc :client (d/client datomic-config)))

(>defn- reset-datomic-db-name!
  "Sets a new database-name for transactions."
  [database-name]
  [string? :ret any?]
  (swap! datomic-info assoc :database-name database-name))

(defn new-connection
  "Connects to the database and returns a connection."
  []
  (let [{:keys [client database-name]} @datomic-info]
    (d/connect client {:db-name database-name})))

(defn transact
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
  []
  [:ret boolean?]
  (let [{:keys [client database-name]} @datomic-info]
    (d/create-database
      client
      {:db-name database-name})))

(>defn delete-database!
  "Delete a database by its name."
  []
  [:ret boolean?]
  (let [{:keys [client database-name]} @datomic-info]
    (d/delete-database
      client
      {:db-name database-name})))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init! {:datomic config/datomic
           :name config/db-name}))
  ([config]
   (reset-datomic-client! (:datomic config))
   (reset-datomic-db-name! (:name config))
   (when-not (= :peer-server (-> (:datomic config) :server-type))
     (create-database!))
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
   (transact test-data/schnaq-test-data)))


;; -----------------------------------------------------------------------------
;; Pull Patterns

(def ^:private author-pattern
  "Pull an author based on these attributes."
  [:db/id
   :author/nickname])

(def ^:private user-pattern
  "Pull a user based on these atributes"
  [:db/id
   {:user/core-author [:author/nickname]}
   :user/upvotes
   :user/downvotes])

(def ^:private meeting-pattern-public
  "Pull a schnaq based on these attributes, omit sensitive information"
  [:db/id
   :meeting/title
   :meeting/start-date
   :meeting/end-date
   :meeting/description
   :meeting/share-hash
   {:meeting/author author-pattern}])

(def ^:private graph-statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   [:statement/content :as :content]
   {:statement/author [:author/nickname]}])

(def ^:private meeting-pattern
  "Has all meeting information, including sensitive ones."
  (conj meeting-pattern-public :meeting/edit-hash))


;; ##### Input functions #####
(defn now [] (Date.))

(>defn- clean-db-vals
  "Removes all entries from a map that have a value of nil or empty string."
  [data]
  [associative? :ret associative?]
  (into {} (remove #(or (nil? (second %))
                        (when (= String (type (second %)))
                          (string/blank? (second %))))
                   data)))

(>defn- clean-and-add-to-db!
  "Removes empty strings and nil values from map before transacting it to the
  database. Checks if the specification still matches. If true, transact the
  entity."
  [entity spec]
  [associative? keyword? :ret int?]
  (let [clean-entity (clean-db-vals entity)
        identifier (format "new-entity-%s"
                           (.toString (UUID/randomUUID)))]
    (when (s/valid? spec clean-entity)
      (get-in
        (transact [(assoc clean-entity :db/id identifier)])
        [:tempids identifier]))))

(>defn clean-and-update-db!
  "Removes empty strings and nil values from map before transacting it to the
  database. Checks if the specification still matches. If true, transact the
  entity."
  [entity spec]
  [associative? keyword? :ret int?]
  (let [clean-entity (clean-db-vals entity)]
    (when (s/valid? spec clean-entity)
      (transact [clean-entity])
      (:db/id entity))))


;; -----------------------------------------------------------------------------
;; Feedbacks

(>defn add-feedback!
  "Adds a feedback to the database. Returns the id of the newly added feedback."
  [feedback]
  [::models/feedback :ret int?]
  (clean-and-add-to-db! feedback ::models/feedback))

(defn all-feedbacks
  "Return complete feedbacks from database."
  []
  (d/q
    '[:find (pull ?feedback [*])
      :where [?feedback :feedback/description]]
    (d/db (new-connection))))


;; -----------------------------------------------------------------------------
;; Meetings

(>defn add-meeting
  "Adds a meeting to the database. Returns the id of the newly added meeting.
  Automatically cleans input."
  [meeting]
  [map? :ret int?]
  (clean-and-add-to-db! meeting ::models/meeting))

(>defn update-meeting
  "Updates a meeting. Returns the id of the newly updated meeting.
  Automatically cleans input. Update of hashes is not allowed."
  [meeting]
  [map? :ret int?]
  (clean-and-update-db! meeting ::models/meeting-without-hashes))

(>defn update-agenda
  "Updates an agenda. Object must be complete with all required attributes."
  [agenda]
  [map? :ret int?]
  (clean-and-update-db! (dissoc agenda :agenda/discussion) ::models/agenda-without-discussion))

(>defn meeting-private-data
  "Return non public meeting data by id."
  [id]
  [int? :ret ::models/meeting]
  (d/pull (d/db (new-connection)) meeting-pattern id))

(defn all-meetings
  "Shows all meetings currently in the db."
  []
  (d/q
    '[:find (pull ?meetings meeting-pattern-public)
      :in $ meeting-pattern-public
      :where [?meetings :meeting/title _]]
    (d/db (new-connection)) meeting-pattern-public))

(>defn meeting-by-hash-generic
  "Generic meeting by hash method, outputs according to pattern."
  [hash pattern]
  [string? sequential? :ret map?]
  (ffirst
    (d/q
      '[:find (pull ?meeting pattern)
        :in $ ?hash pattern
        :where [?meeting :meeting/share-hash ?hash]]
      (d/db (new-connection)) hash pattern)))

(defn meeting-by-hash
  "Returns the meeting corresponding to the share hash."
  [hash]
  (meeting-by-hash-generic hash meeting-pattern-public))

(defn meeting-by-hash-private
  "Returns all meeting data, even the private parts by hash."
  [hash]
  (meeting-by-hash-generic hash meeting-pattern))

;; ----------------------------------------------------------------------------
;; agenda
;; ----------------------------------------------------------------------------

(>defn add-agenda-point
  "Add an agenda to the database.
  A discussion is automatically created for the agenda-point.
  Returns the discussion-id of the newly created discussion."
  [title description meeting-id]
  [:agenda/title (? string?) int? :ret int?]
  (when (and (s/valid? :agenda/title title)
             (s/valid? int? meeting-id))
    (let [raw-agenda {:db/id "temp-id"
                      :agenda/title title
                      :agenda/meeting meeting-id
                      :agenda/discussion
                      {:db/id "whatever-forget-it"
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

(>defn agenda
  "Return agenda data by id."
  [id]
  [int? :ret ::models/agenda]
  (d/pull (d/db (new-connection)) agenda-pattern id))

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

;; ----------------------------------------------------------------------------
;; user
;; ----------------------------------------------------------------------------

(>defn user [id]
  [int? :ret map?]
  (d/pull (d/db (new-connection)) user-pattern id))

(>defn author-id-by-nickname
  "Returns an author-id by nickname. The nickname is not case sensitive"
  [nickname]
  [string? :ret (? number?)]
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

(>defn user-by-nickname
  "Return the **schnaq** user-id by nickname. The nickname is not case sensitive."
  [nickname]
  [string? :ret (? number?)]
  (let [dialog-author (author-id-by-nickname nickname)]
    (when-not (nil? dialog-author)
      (ffirst
        (d/q
          '[:find ?user
            :in $ ?author
            :where [?user :user/core-author ?author]]
          (d/db (new-connection)) dialog-author)))))

(>defn canonical-username
  "Return the canonical username (regarding case) that is saved."
  [nickname]
  [:author/nickname :ret :author/nickname]
  (:author/nickname
    (d/pull (d/db (new-connection)) [:author/nickname] (author-id-by-nickname nickname))))

(>defn all-statements-for-discussion
  "Returns all statements for a discussion. Specially prepared for node and edge generation."
  [discussion-id]
  [int? :ret sequential?]
  (map
    (fn [[statement & _]]
      {:author (-> statement :statement/author :author/nickname)
       :id (:db/id statement)
       :label (:content statement)})
    (d/q
      '[:find (pull ?statements statement-pattern)
        :in $ ?discussion-id statement-pattern
        :where [?arguments :argument/discussions ?discussion-id]
        [?statements :statement/version _]
        (or
          [?arguments :argument/conclusion ?statements]
          [?arguments :argument/premises ?statements])]
      (d/db (new-connection)) discussion-id graph-statement-pattern)))

(>defn add-user-if-not-exists
  "Adds an author if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:author/nickname :ret int?]
  (if-let [user-id (user-by-nickname nickname)]
    user-id
    (add-user nickname)))

(>defn all-author-names
  "Returns the names of all authors."
  []
  [:ret (s/coll-of :author/nickname)]
  (map first
       (d/q
         '[:find ?names
           :where [_ :author/nickname ?names]]
         (d/db (new-connection)))))

;; ----------------------------------------------------------------------------
;; voting
;; ----------------------------------------------------------------------------

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

(>defn delete-agendas
  "Remove all agendas. Check for id belonging to a meeting before removing."
  [agenda-ids meeting-id]
  [(s/coll-of int?) int? :ret (? map?)]
  (let [corresponding-meetings (map #(d/pull (d/db (new-connection)) [:agenda/meeting] %) agenda-ids)
        checked-agendas (filter #(= meeting-id (get-in % [:agenda/meeting :db/id])) corresponding-meetings)]
    (when (= (count corresponding-meetings) (count checked-agendas))
      (transact (mapv #(vector :db/retractEntity %) agenda-ids)))))

;; ##### From here on  Analytics. This will be refactored into its own app sometime.###################

(def ^:private max-time-back #inst "1971-01-01T01:01:01.000-00:00")

(>defn- number-of-entities-since
  "Returns the number of entities in the db since some timestamp. Default is all."
  ([attribute]
   [keyword? :ret int?]
   (number-of-entities-since attribute max-time-back))
  ([attribute since]
   [keyword? inst? :ret int?]
   (or
     (ffirst
       (d/q
         '[:find (count ?entities)
           :in $ ?since ?attribute
           :where [?entities ?attribute _ ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (d/db (new-connection)) since attribute))
     0)))

(>defn- number-of-entities-with-value-since
  "Returns the number of entities in the db since some timestamp. Default is all."
  ([attribute value]
   [keyword? any? :ret int?]
   (number-of-entities-with-value-since attribute value max-time-back))
  ([attribute value since]
   [keyword? any? inst? :ret int?]
   (or
     (ffirst
       (d/q
         '[:find (count ?entities)
           :in $ ?since ?attribute ?value
           :where [?entities ?attribute ?value ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (d/db (new-connection)) since attribute value))
     0)))

(defn number-of-meetings
  "Returns the number of meetings. Optionally takes a date since when this counts."
  ([] (number-of-entities-since :meeting/title))
  ([since] (number-of-entities-since :meeting/title since)))

(defn number-of-usernames
  "Returns the number of different usernames in the database."
  ([] (number-of-entities-since :author/nickname))
  ([since] (number-of-entities-since :author/nickname since)))

(defn number-of-statements
  "Returns the number of different usernames in the database."
  ([] (number-of-entities-since :statement/content))
  ([since] (number-of-entities-since :statement/content since)))

(>defn average-number-of-agendas
  "Returns the average number of agendas per discussion."
  ([]
   [:ret number?]
   (average-number-of-agendas max-time-back))
  ([since]
   [inst? :ret number?]
   (let [meetings (number-of-meetings since)
         agendas (number-of-entities-since :agenda/title since)]
     (/ agendas meetings))))

(>defn number-of-active-users
  "Returns the number of active users (With at least one statement)."
  ([]
   [:ret int?]
   (number-of-active-users max-time-back))
  ([since]
   [inst? :ret int?]
   (or
     (ffirst
       (d/q
         '[:find (count ?authors)
           :in $ ?since
           :where [?authors :author/nickname _ ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]
           [?statements :statement/author ?authors]]
         (d/db (new-connection)) since))
     0)))

(>defn statement-length-stats
  "Returns a map of stats about statement-length."
  ([] [:ret map?]
   (statement-length-stats max-time-back))
  ([since] [inst? :ret map?]
   (let [sorted-contents (sort-by count
                                  (flatten
                                    (d/q
                                      '[:find ?contents
                                        :in $ ?since
                                        :where [_ :statement/content ?contents ?tx]
                                        [?tx :db/txInstant ?add-date]
                                        [(< ?since ?add-date)]]
                                      (d/db (new-connection)) since)))
         content-count (count sorted-contents)
         max-length (count (last sorted-contents))
         min-length (count (first sorted-contents))
         average-length (float (/ (reduce #(+ %1 (count %2)) 0 sorted-contents) content-count))
         median-length (count (nth sorted-contents (quot content-count 2)))]
     {:max max-length
      :min min-length
      :average average-length
      :median median-length})))

(>defn argument-type-stats
  "Returns the number of attacks, supports and undercuts since a certain timestamp."
  ([] [:ret map?]
   (argument-type-stats max-time-back))
  ([since] [inst? :ret map?]
   {:supports (number-of-entities-with-value-since :argument/type :argument.type/support since)
    :attacks (number-of-entities-with-value-since :argument/type :argument.type/attack since)
    :undercuts (number-of-entities-with-value-since :argument/type :argument.type/undercut since)}))
