(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [datomic.client.api :as d]
            [ghostwheel.core :refer [>defn ? >defn-]]
            [schnaq.meeting.database :refer [transact] :as main-db]
            [schnaq.meeting.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (clojure.lang ExceptionInfo)))

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

(>defn- react-to-statement!
  "Create a new statement reacting to another statement. Returns the newly created argument."
  [share-hash author-id statement-id reacting-string reaction]
  [:meeting/share-hash :db/id :db/id :statement/content keyword? :ret ::specs/argument]
  (let [argument-id
        (get-in
          (main-db/new-premises-for-statement! share-hash author-id statement-id reacting-string reaction)
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
