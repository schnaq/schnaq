(ns schnaq.meeting.database
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [datomic.client.api :as d]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.config :as config]
            [schnaq.meeting.models :as models]
            [schnaq.meeting.specs :as specs]
            [schnaq.test-data :as test-data]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (java.util UUID Date)
           (java.time Instant)))

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

(defn- query
  "Shorthand to not type out the same first param every time"
  [query-vector & args]
  (apply d/q query-vector (d/db (new-connection)) args))

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
  "Initializing the datomic database and feeding it with test-data.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init-and-seed! {:datomic config/datomic
                    :name config/db-name}))
  ([config]
   (init! config)
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
   :meeting/type
   :meeting/description
   :meeting/share-hash
   {:meeting/author author-pattern}])

(def ^:private statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :statement/content
   :statement/version
   :statement/deleted?
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

(defn fast-pull
  "Pulls any entity with star-syntax and current db."
  [id]
  (d/pull (d/db (new-connection)) '[*] id))

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

;; -----------------------------------------------------------------------------
;; Feedbacks

(>defn add-feedback!
  "Adds a feedback to the database. Returns the id of the newly added feedback."
  [feedback]
  [::specs/feedback :ret int?]
  (clean-and-add-to-db! feedback ::specs/feedback))

(defn all-feedbacks
  "Return complete feedbacks from database, sorted by descending timestamp."
  []
  (map first
       (sort-by
         second toolbelt/comp-compare
         (d/q
           '[:find (pull ?feedback [*]) ?ts
             :where [?feedback :feedback/description _ ?tx]
             [?tx :db/txInstant ?ts]]
           (d/db (new-connection))))))

;; -----------------------------------------------------------------------------
;; Meetings

(>defn add-meeting
  "Adds a meeting to the database. Returns the id of the newly added meeting.
  Automatically cleans input."
  [meeting]
  [map? :ret int?]
  (clean-and-add-to-db! meeting ::specs/meeting))

(>defn meeting-private-data
  "Return non public meeting data by id."
  [id]
  [int? :ret ::specs/meeting]
  (d/pull (d/db (new-connection)) meeting-pattern id))

(defn all-meetings
  "Shows all meetings currently in the db."
  []
  (->
    (d/q
      '[:find (pull ?meetings meeting-pattern-public)
        :in $ meeting-pattern-public
        :where [?meetings :meeting/title _]]
      (d/db (new-connection)) meeting-pattern-public)
    (toolbelt/pull-key-up :db/ident)
    flatten))

(>defn meeting-by-hash-generic
  "Generic meeting by hash method, outputs according to pattern."
  [hash pattern]
  [string? sequential? :ret map?]
  (->
    (d/q
      '[:find (pull ?meeting pattern)
        :in $ ?hash pattern
        :where [?meeting :meeting/share-hash ?hash]]
      (d/db (new-connection)) hash pattern)
    ffirst
    (toolbelt/pull-key-up :db/ident)))

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
  ([title description meeting-id]
   [:agenda/title (? string?) int? :ret int?]
   (add-agenda-point title description meeting-id 1))
  ([title description meeting-id rank]
   [:agenda/title (? string?) int? :agenda/rank :ret int?]
   (when (and (s/valid? :agenda/title title)
              (s/valid? int? meeting-id))
     (let [raw-agenda {:db/id "temp-id"
                       :agenda/title title
                       :agenda/meeting meeting-id
                       :agenda/rank rank
                       :agenda/discussion
                       {:db/id "whatever-forget-it"
                        :discussion/title title
                        :discussion/states [:discussion.state/open]
                        :discussion/starting-statements []}}
           agenda (if (and description (s/valid? :agenda/description description))
                    (merge-with merge
                                raw-agenda
                                {:agenda/description description
                                 :agenda/discussion {:discussion/description description}})
                    raw-agenda)]
       (get-in
         (transact [agenda])
         [:tempids "temp-id"])))))

(def ^:private agenda-pattern
  [:db/id
   :agenda/title
   :agenda/description
   :agenda/meeting
   :agenda/rank
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

(>defn all-statements
  "Returns all statements belonging to a discussion"
  [discussion-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (distinct
    (concat
      (flatten
        (d/q
          '[:find (pull ?statements statement-pattern)
            :in $ ?discussion-id statement-pattern
            :where [?arguments :argument/discussions ?discussion-id]
            [?statements :statement/version _]
            (or
              [?arguments :argument/conclusion ?statements]
              [?arguments :argument/premises ?statements])]
          (d/db (new-connection)) discussion-id statement-pattern))
      (flatten
        (d/q
          '[:find (pull ?statements statement-pattern)
            :in $ ?discussion-id statement-pattern
            :where [?discussion-id :discussion/starting-statements ?statements]]
          (d/db (new-connection)) discussion-id statement-pattern)))))

(>defn number-of-statements-for-discussion
  "Returns number of statements for a discussion-id."
  [discussion-id]
  [int? :ret int?]
  (count (all-statements discussion-id)))

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
   :ret ::specs/agenda]
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
  (when-let [dialog-author (author-id-by-nickname nickname)]
    (ffirst
      (d/q
        '[:find ?user
          :in $ ?author
          :where [?user :user/core-author ?author]]
        (d/db (new-connection)) dialog-author))))

(>defn canonical-username
  "Return the canonical username (regarding case) that is saved."
  [nickname]
  [:author/nickname :ret :author/nickname]
  (:author/nickname
    (d/pull (d/db (new-connection)) [:author/nickname] (author-id-by-nickname nickname))))

(>defn all-statements-for-graph
  "Returns all statements for a discussion. Specially prepared for node and edge generation."
  [discussion-id]
  [int? :ret sequential?]
  (map
    (fn [statement]
      {:author (-> statement :statement/author :author/nickname)
       :id (:db/id statement)
       :label (if (:statement/deleted? statement)
                config/deleted-statement-text
                (:statement/content statement))})
    (all-statements discussion-id)))

(>defn add-user-if-not-exists
  "Adds an author and user if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:author/nickname :ret int?]
  (if-let [user-id (user-by-nickname nickname)]
    user-id
    (do (log/info "Added a new user: " nickname)
        (add-user nickname))))

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
  (or
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
        (d/db (new-connection)) statement-id meeting-hash))
    (ffirst
      (d/q
        '[:find ?meeting
          :in $ ?statement ?hash
          :where [?discussion :discussion/starting-statements ?statement]
          [?agenda :agenda/discussion ?discussion]
          [?agenda :agenda/meeting ?meeting]
          [?meeting :meeting/share-hash ?hash]]
        (d/db (new-connection)) statement-id meeting-hash))))

;; ##### From here on  Analytics. This will be refactored into its own app sometime.###################

(def ^:private max-time-back Instant/EPOCH)

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
         (d/db (new-connection)) (Date/from since) attribute))
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
         (d/db (new-connection)) (Date/from since) attribute value))
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
     (if (zero? meetings)
       0
       (/ agendas meetings)))))

(>defn active-discussion-authors
  "Returns all authors active in a discussion during a period since the provided
  timestamp."
  [since]
  [inst? :ret sequential?]
  (flatten
    (d/q
      '[:find ?authors
        :in $ ?since
        :where [?statements :statement/author ?authors ?tx]
        [?tx :db/txInstant ?start-date]
        [(< ?since ?start-date)]]
      (d/db (new-connection)) (Date/from since))))

(>defn active-preparation-authors
  "Returns all authors active in a the meeting preparation in a period since the
  provided timestamp."
  [since]
  [inst? :ret sequential?]
  (flatten
    (d/q
      '[:find ?authors
        :in $ ?since
        :where
        (or
          [?_suggestion :agenda.suggestion/ideator ?users ?tx]
          [?_suggestion :meeting.suggestion/ideator ?users ?tx])
        [?tx :db/txInstant ?start-date]
        [(< ?since ?start-date)]
        [?users :user/core-author ?authors]]
      (d/db (new-connection)) (Date/from since))))

(>defn number-of-active-discussion-users
  "Returns the number of active users (With at least one statement or suggestion)."
  ([]
   [:ret int?]
   (number-of-active-discussion-users max-time-back))
  ([since]
   [inst? :ret int?]
   (let [discussion-authors (active-discussion-authors since)
         preparation-authors (active-preparation-authors since)]
     (count (set (concat discussion-authors preparation-authors))))))

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
                                      (d/db (new-connection)) (Date/from since))))
         content-count (count sorted-contents)
         max-length (count (last sorted-contents))
         min-length (count (first sorted-contents))
         average-length (if (zero? content-count) 0 (float (/ (reduce #(+ %1 (count %2)) 0 sorted-contents) content-count)))
         median-length (if (zero? content-count) 0 (count (nth sorted-contents (quot content-count 2))))]
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

;; Dialog.core outfactor. Should Probably go into its own namespace on next refactor.

(def ^:private argument-pattern
  "Defines the default pattern for arguments. Oftentimes used in pull-patterns
  in a Datalog query bind the data to this structure."
  [:db/id
   :argument/version
   {:argument/author [:author/nickname]}
   {:argument/type [:db/ident]}
   {:argument/premises statement-pattern}
   {:argument/conclusion
    (conj statement-pattern
          :argument/version
          {:argument/author [:author/nickname]}
          {:argument/type [:db/ident]}
          {:argument/premises [:db/id
                               :statement/content
                               :statement/version
                               {:statement/author [:author/nickname]}]}
          {:argument/conclusion statement-pattern})}])

(def ^:private discussion-pattern
  "Representation of a discussion. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :discussion/title
   :discussion/description
   {:discussion/states [:db/ident]}
   {:discussion/starting-arguments argument-pattern}
   {:discussion/starting-statements statement-pattern}])

(>defn get-statement
  "Returns the statement given an id."
  [statement-id]
  [:db/id :ret ::specs/statement]
  (d/pull (d/db (new-connection)) statement-pattern statement-id))

(>defn starting-conclusions-by-discussion
  {:deprecated "Use `starting-statements` instead"
   :doc "Query all conclusions belonging to starting-arguments of a certain discussion."}
  [discussion-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (flatten
    (query
      '[:find (pull ?starting-conclusions statement-pattern)
        :in $ ?discussion-id statement-pattern
        :where [?discussion-id :discussion/starting-arguments ?starting-arguments]
        [?starting-arguments :argument/conclusion ?starting-conclusions]]
      discussion-id statement-pattern)))

(defn starting-arguments-by-discussion
  {:deprecated "2020-11-03"
   :doc "Do not use this function anymore in production.
   Deep-Query all starting-arguments of a certain discussion."}
  [discussion-id]
  (-> (query
        '[:find (pull ?starting-arguments argument-pattern)
          :in $ argument-pattern ?discussion-id
          :where [?discussion-id :discussion/starting-arguments ?starting-arguments]]
        argument-pattern discussion-id)
      flatten
      (toolbelt/pull-key-up :db/ident)))

(>defn starting-statements
  "Returns all starting-statements belonging to a discussion."
  [discussion-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (flatten
    (query
      '[:find (pull ?statements statement-pattern)
        :in $ ?discussion-id statement-pattern
        :where [?discussion-id :discussion/starting-statements ?statements]]
      discussion-id statement-pattern)))

(defn- discussions-by-share-hash
  "Returns all discussions which can be reached by a certain share-hash."
  [share-hash]
  (flatten
    (query
      '[:find ?discussions
        :in $ ?share-hash
        :where [?meeting :meeting/share-hash ?share-hash]
        [?agenda :agenda/meeting ?meeting]
        [?agenda :agenda/discussion ?discussions]]
      share-hash)))

(>defn statements-belong-to-discussion?
  "Returns whether the statements belong to a discussion identified by the share-hash."
  [statement-ids share-hash]
  [sequential? :meeting/share-hash :ret boolean?]
  (let [all-discussions (discussions-by-share-hash share-hash)
        reachable-statements (->> all-discussions
                                  (map all-statements)
                                  flatten
                                  (map :db/id)
                                  set)]
    (every? #(contains? reachable-statements %) statement-ids)))

(>defn delete-statements!
  "Deletes all statements, without explicitly checking anything."
  [statement-ids]
  [(s/coll-of :db/id) :ret associative?]
  (transact (mapv #(vector :db/add % :statement/deleted? true) statement-ids)))

(>defn- pack-premises
  "Packs premises into a statement-structure."
  [premises author-id]
  [(s/coll-of :statement/content) :db/id
   :ret (s/coll-of map?)]
  (mapv (fn [premise] {:db/id (str "premise-" premise)
                       :statement/author author-id
                       :statement/content premise
                       :statement/version 1})
        premises))

(>defn- prepare-new-argument
  "Prepares a new argument for transaction. Optionally sets a temporary id."
  ([discussion-id author-id conclusion premises temporary-id]
   [:db/id :db/id :statement/content (s/coll-of :statement/content) :db/id :ret map?]
   (merge
     (prepare-new-argument discussion-id author-id conclusion premises)
     {:db/id temporary-id}))
  ([discussion-id author-id conclusion premises]
   [:db/id :db/id :statement/content (s/coll-of :statement/content) :ret map?]
   {:argument/author author-id
    :argument/premises (pack-premises premises author-id)
    :argument/conclusion {:db/id (str "conclusion-" conclusion)
                          :statement/author author-id
                          :statement/content conclusion
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions [discussion-id]}))

(defn- build-new-statement
  "Builds a new statement for transaction."
  ([author-id content]
   (build-new-statement author-id content (str "conclusion-" content)))
  ([author-id content temp-id]
   {:db/id temp-id
    :statement/author author-id
    :statement/content content
    :statement/version 1}))

(>defn add-starting-statement!
  "Adds a new starting-statement and returns the newly created id."
  [discussion-id author-id statement-content]
  [:db/id :db/id :statement/content :ret :db/id]
  (let [new-statement (build-new-statement author-id statement-content "add/starting-argument")
        temporary-id (:db/id new-statement)]
    (get-in (transact [new-statement
                       [:db/add discussion-id :discussion/starting-statements temporary-id]])
            [:tempids temporary-id])))

(defn all-arguments-for-conclusion
  "Get all arguments for a given conclusion."
  [conclusion-id]
  (-> (query
        '[:find (pull ?arguments argument-pattern)
          :in $ argument-pattern ?conclusion
          :where [?arguments :argument/conclusion ?conclusion]]
        argument-pattern conclusion-id)
      (toolbelt/pull-key-up :db/ident)
      flatten))

(defn statements-undercutting-premise
  "Return all statements that are used to undercut an argument where `statement-id`
  is used as one of the premises in the undercut argument."
  [statement-id]
  (flatten
    (query
      '[:find (pull ?undercutting-statements statement-pattern)
        :in $ statement-pattern ?statement-id
        :where [?arguments :argument/premises ?statement-id]
        [?undercutting-arguments :argument/conclusion ?arguments]
        [?undercutting-arguments :argument/premises ?undercutting-statements]]
      statement-pattern statement-id)))

(>defn all-discussions-by-title
  "Query all discussions based on the title. Could possible be multiple
  entities."
  [title]
  [string? :ret (s/coll-of ::specs/discussion)]
  (flatten
    (query
      '[:find (pull ?discussions discussion-pattern)
        :in $ discussion-pattern ?title
        :where [?discussions :discussion/title ?title]]
      discussion-pattern title)))

(defn all-arguments-for-discussion
  "Returns all arguments belonging to a discussion, identified by discussion id."
  [discussion-id]
  (-> (query
        '[:find (pull ?discussion-arguments argument-pattern)
          :in $ argument-pattern ?discussion-id
          :where [?discussion-arguments :argument/discussions ?discussion-id]]
        argument-pattern discussion-id)
      flatten
      (toolbelt/pull-key-up :db/ident)))

(>defn statements-by-content
  "Returns all statements that have the matching `content`."
  [content]
  [:statement/content
   :ret (s/coll-of ::specs/statement)]
  (map first
       (let [db (d/db (new-connection))]
         (d/q
           '[:find (pull ?statements statement-pattern)
             :in $ statement-pattern ?content
             :where [?statements :statement/content ?content]]
           db statement-pattern content))))

(>defn argument-id-by-premise-conclusion
  "Return the ID of an argument, which has at least the corresponding premise and
  conclusion. If multiple are applicable, return any of them."
  [premise-id conclusion-id]
  [number? number? :ret (s/nilable number?)]
  (:db/id
    (ffirst
      (query
        '[:find (pull ?argument argument-pattern)
          :in $ argument-pattern ?premise-id ?conclusion-id
          :where [?argument :argument/premises ?premise-id]
          [?argument :argument/conclusion ?conclusion-id]]
        argument-pattern premise-id conclusion-id))))

(>defn argument-id-by-undercut-and-premise
  "Returns one argument that has a premise `undercut-premise-id` and which has a conclusion
  that has a premise which contains `conclusion-premise-id`. Basically identifies an undercut by the premise
  and the conclusions premise."
  [undercut-premise-id conclusion-premise-id]
  [:db/id :db/id :ret :db/id]
  (:db/id
    (ffirst
      (query
        '[:find (pull ?undercut argument-pattern)
          :in $ argument-pattern ?undercut-premise-id ?conclusion-premise-id
          :where [?undercut :argument/premises ?undercut-premise-id]
          [?undercut :argument/conclusion ?undercutted-argument]
          [?undercutted-argument :argument/premises ?conclusion-premise-id]]
        argument-pattern undercut-premise-id conclusion-premise-id))))

(>defn- new-premises-for-statement!
  "Creates a new argument based on a statement, which is used as conclusion."
  [discussion-id author-id new-conclusion-id new-statement-string argument-type]
  [:db/id :db/id :db/id :statement/content :argument/type :ret associative?]
  (let [new-arguments
        [{:db/id (str "argument-" new-statement-string)
          :argument/author author-id
          :argument/premises (pack-premises [new-statement-string] author-id)
          :argument/conclusion new-conclusion-id
          :argument/version 1
          :argument/type argument-type
          :argument/discussions [discussion-id]}]]
    (transact new-arguments)))

(>defn- react-to-statement!
  "Create a new statement reacting to another statement. Returns the newly created argument."
  [discussion-id author-id statement-id reacting-string reaction]
  [:db/id :db/id :db/id :statement/content keyword? :ret ::specs/argument]
  (let [argument-id
        (get-in
          (new-premises-for-statement! discussion-id author-id statement-id reacting-string reaction)
          [:tempids (str "argument-" reacting-string)])]
    (toolbelt/pull-key-up
      (d/pull (d/db (new-connection)) argument-pattern argument-id)
      :db/ident)))

(>defn attack-statement!
  "Create a new statement attacking another statement. Returns the newly created argument."
  [discussion-id author-id statement-id attacking-string]
  [:db/id :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! discussion-id author-id statement-id attacking-string :argument.type/attack))

(>defn support-statement!
  "Create a new statement supporting another statement. Returns the newly created argument."
  [discussion-id author-id statement-id supporting-string]
  [:db/id :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! discussion-id author-id statement-id supporting-string :argument.type/support))

(>defn undercut-argument!
  "Undercut an argument and store it to the database."
  [discussion-id author-id argument-id premises]
  [number? :db/id :db/id (s/coll-of :statement/content) :ret ::specs/argument]
  (let [argument-id (get-in
                      (transact
                        [{:db/id (str "new-undercut-" discussion-id)
                          :argument/author author-id
                          :argument/premises (pack-premises premises author-id)
                          :argument/conclusion argument-id
                          :argument/version 1
                          :argument/type :argument.type/undercut
                          :argument/discussions [discussion-id]}])
                      [:tempids (str "new-undercut-" discussion-id)])]
    (toolbelt/pull-key-up
      (d/pull (d/db (new-connection)) argument-pattern argument-id)
      :db/ident)))
