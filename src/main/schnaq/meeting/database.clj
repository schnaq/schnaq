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

(defn query
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
   (init-and-seed! config test-data/schnaq-test-data))
  ([config test-data]
   (init! config)
   (transact test-data)))


;; -----------------------------------------------------------------------------
;; Pull Patterns

(def user-pattern
  "Pull a user based on these attributes"
  [:db/id
   :user/upvotes
   :user/downvotes
   :user/nickname])

(def statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :statement/content
   :statement/version
   :statement/deleted?
   {:statement/author [:user/nickname]}])

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

(>defn clean-and-add-to-db!
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
;; Feedback functions

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

;; ----------------------------------------------------------------------------
;; user
;; ----------------------------------------------------------------------------


(def minimal-user-pattern
  "Minimal user pull pattern."
  [:db/id
   :user/nickname])

(>defn user
  "Pull user from database."
  [id]
  [int? :ret map?]
  (d/pull (d/db (new-connection)) minimal-user-pattern id))

(>defn add-user
  "Add a new user / author to the database."
  [nickname]
  [string? :ret int?]
  (when (s/valid? :user/nickname nickname)
    (get-in
      (transact [{:db/id "temp-user"
                  :user/nickname nickname}])
      [:tempids "temp-user"])))

(>defn user-by-nickname
  "Return the **schnaq** user-id by nickname. The nickname is not case sensitive.
  If there is no user with said nickname returns nil."
  [nickname]
  [string? :ret (? number?)]
  (ffirst
    (d/q
      '[:find ?user
        :in $ ?user-name
        :where [?user :user/nickname ?original-nickname]
        [(.toLowerCase ^String ?original-nickname) ?lower-name]
        [(= ?lower-name ?user-name)]]
      (d/db (new-connection)) (.toLowerCase ^String nickname))))

(>defn canonical-username
  "Return the canonical username (regarding case) that is saved."
  [nickname]
  [:user/nickname :ret :user/nickname]
  (:user/nickname
    (d/pull (d/db (new-connection)) [:user/nickname] (user-by-nickname nickname))))

(>defn add-user-if-not-exists
  "Adds a user if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:user/nickname :ret int?]
  (if-let [user-id (user-by-nickname nickname)]
    user-id
    (do (log/info "Added a new user: " nickname)
        (add-user nickname))))

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
        :where [?user :user/nickname ?nickname]
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

;; ##### From here on  Analytics. This will be refactored into its own app sometime. ###################

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
  ([] (number-of-entities-since :user/nickname))
  ([since] (number-of-entities-since :user/nickname since)))

(defn number-of-statements
  "Returns the number of different usernames in the database."
  ([] (number-of-entities-since :statement/content))
  ([since] (number-of-entities-since :statement/content since)))

(>defn average-number-of-agendas
  "Returns the average number of agendas per discussion."
  ([]
   [:ret number?]
   (average-number-of-agendas max-time-back))
  ([_since]
   [inst? :ret number?]
   ;; todo rework with analytics refactor
   #_(let [meetings (number-of-meetings since)
         agendas (number-of-entities-since :agenda/title since)]
     (if (zero? meetings)
       0
       (/ agendas meetings)))
   0))

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

(>defn number-of-active-discussion-users
  "Returns the number of active users (With at least one statement or suggestion)."
  ([]
   [:ret int?]
   (number-of-active-discussion-users max-time-back))
  ([since]
   [inst? :ret int?]
   (let [discussion-authors (active-discussion-authors since)]
     (count (set discussion-authors)))))

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
