(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [ghostwheel.core :refer [>defn ?]]
            [schnaq.meeting.database :refer [transact] :as main-db]
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
