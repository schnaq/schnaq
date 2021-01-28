(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [clojure.spec.alpha :as s]
            [datomic.client.api :as d]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.meeting.database :refer [transact new-connection query] :as main-db]
            [schnaq.meeting.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (clojure.lang ExceptionInfo)))

(def argument-pattern
  "Defines the default pattern for arguments. Oftentimes used in pull-patterns
  in a Datalog query bind the data to this structure."
  [:db/id
   :argument/version
   {:argument/author [:author/nickname]}
   {:argument/type [:db/ident]}
   {:argument/premises main-db/statement-pattern}
   {:argument/conclusion
    (conj main-db/statement-pattern
          :argument/version
          {:argument/author [:author/nickname]}
          {:argument/type [:db/ident]}
          {:argument/premises [:db/id
                               :statement/content
                               :statement/version
                               {:statement/author [:author/nickname]}]}
          {:argument/conclusion main-db/statement-pattern})}])

(def discussion-pattern
  "Representation of a discussion. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :discussion/title
   :discussion/description
   {:discussion/states [:db/ident]}
   {:discussion/starting-arguments argument-pattern}
   {:discussion/starting-statements main-db/statement-pattern}])

(>defn get-statement
  "Returns the statement given an id."
  [statement-id]
  [:db/id :ret ::specs/statement]
  (d/pull (d/db (new-connection)) main-db/statement-pattern statement-id))

(>defn starting-statements
  "Returns all starting-statements belonging to a discussion."
  [share-hash]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (flatten
    (query
      '[:find (pull ?statements statement-pattern)
        :in $ ?share-hash statement-pattern
        :where [?meeting :meeting/share-hash ?share-hash]
        [?agenda :agenda/meeting ?meeting]
        [?agenda :agenda/discussion ?discussion]
        [?discussion :discussion/starting-statements ?statements]]
      share-hash main-db/statement-pattern)))

(defn discussion-by-share-hash
  "Returns one discussion which can be reached by a certain share-hash. (Brainstorm only ever have one)"
  [share-hash]
  (ffirst
    (query
      '[:find ?discussions
        :in $ ?share-hash
        :where [?meeting :meeting/share-hash ?share-hash]
        [?agenda :agenda/meeting ?meeting]
        [?agenda :agenda/discussion ?discussions]]
      share-hash)))

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

(>defn prepare-new-argument
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
  [share-hash author-id statement-content]
  [:meeting/share-hash :db/id :statement/content :ret :db/id]
  (let [new-statement (build-new-statement author-id statement-content "add/starting-argument")
        temporary-id (:db/id new-statement)
        discussion-id (discussion-by-share-hash share-hash)]
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
      main-db/statement-pattern statement-id)))

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
  [share-hash]
  (-> (query
        '[:find (pull ?discussion-arguments argument-pattern)
          :in $ argument-pattern ?share-hash
          :where [?meeting :meeting/share-hash ?share-hash]
          [?agenda :agenda/meeting ?meeting]
          [?agenda :agenda/discussion ?discussion]
          [?discussion-arguments :argument/discussions ?discussion]]
        argument-pattern share-hash)
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
           db main-db/statement-pattern content))))

(>defn delete-discussion
  "Adds the deleted state to a discussion"
  [share-hash]
  [:meeting/share-hash :ret (? :meeting/share-hash)]
  (let [discussion-id (discussion-by-share-hash share-hash)]
    (try
      (transact [[:db/add discussion-id :discussion/states :discussion.state/deleted]])
      (log/info "Schnaq with share-hash " share-hash " has been set to deleted.")
      share-hash
      (catch ExceptionInfo e
        (log/error "Deletion of discussion with id " discussion-id " and share-hash "
                   share-hash " failed. Exception:\n" e)))))

(>defn discussion-deleted?
  "Returns whether a discussion has been marked as deleted."
  [share-hash]
  [:meeting/share-hash :ret boolean?]
  (as-> (main-db/query
          '[:find (pull ?states [*])
            :in $ ?share-hash
            :where [?meeting :meeting/share-hash ?share-hash]
            [?agenda :agenda/meeting ?meeting]
            [?agenda :agenda/discussion ?discussions]
            [?discussions :discussion/states ?states]]
          share-hash) q
        (map #(:db/ident (first %)) q)
        (into #{} q)
        (contains? q :discussion.state/deleted)))

(>defn- new-premises-for-statement!
  "Creates a new argument based on a statement, which is used as conclusion."
  [share-hash author-id new-conclusion-id new-statement-string argument-type]
  [:meeting/share-hash :db/id :db/id :statement/content :argument/type :ret associative?]
  (let [discussion-id (discussion-by-share-hash share-hash)
        new-arguments
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
  [share-hash author-id statement-id reacting-string reaction]
  [:meeting/share-hash :db/id :db/id :statement/content keyword? :ret ::specs/argument]
  (let [argument-id
        (get-in
          (new-premises-for-statement! share-hash author-id statement-id reacting-string reaction)
          [:tempids (str "argument-" reacting-string)])]
    (toolbelt/pull-key-up
      (d/pull (d/db (main-db/new-connection)) argument-pattern argument-id)
      :db/ident)))

(>defn support-statement!
  "Create a new statement supporting another statement. Returns the newly created argument."
  [share-hash author-id statement-id supporting-string]
  [:meeting/share-hash :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! share-hash author-id statement-id supporting-string :argument.type/support))

(>defn attack-statement!
  "Create a new statement attacking another statement. Returns the newly created argument."
  [share-hash author-id statement-id attacking-string]
  [:meeting/share-hash :db/id :db/id :statement/content :ret ::specs/argument]
  (react-to-statement! share-hash author-id statement-id attacking-string :argument.type/attack))
