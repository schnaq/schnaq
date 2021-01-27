(ns schnaq.database.discussion
  "Discussion related functions interacting with the database."
  (:require [ghostwheel.core :refer [>defn]]
            [schnaq.meeting.database :refer [transact] :as main-db]
            [taoensso.timbre :as log])
  (:import (clojure.lang ExceptionInfo)))

(>defn delete-discussion
  "Adds the deleted state to a discussion"
  [share-hash]
  [:meeting/share-hash :ret any?]
  (let [discussion-id (main-db/discussion-by-share-hash share-hash)]
    (try
      (transact [[:db/add discussion-id :discussion/states :discussion.state/deleted]])
      (catch ExceptionInfo e
        (log/error "Deletion of discussion with id " discussion-id " and share-hash "
                   share-hash " failed. Exception:\n" e)))))
