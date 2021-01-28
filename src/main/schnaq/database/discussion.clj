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
        main-db/argument-pattern share-hash)
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
  (let [discussion-id (main-db/discussion-by-share-hash share-hash)]
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
  (let [discussion-id (main-db/discussion-by-share-hash share-hash)
        new-arguments
        [{:db/id (str "argument-" new-statement-string)
          :argument/author author-id
          :argument/premises (main-db/pack-premises [new-statement-string] author-id)
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
      (d/pull (d/db (main-db/new-connection)) main-db/argument-pattern argument-id)
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
