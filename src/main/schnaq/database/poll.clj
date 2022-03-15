(ns schnaq.database.poll
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [schnaq.database.main :as db :refer [query]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as tools])
  (:import (java.util UUID)))

(>defn new-poll!
  "Create and return a poll entity. Options must be passed as a collection of strings."
  [title poll-type options discussion-id]
  [:poll/title :poll/type (s/coll-of ::specs/non-blank-string) :db/id :ret (? ::specs/poll)]
  (when (< 0 (count options))
    (tools/pull-key-up
     (db/transact-and-pull-temp
      [{:db/id "newly-created-poll"
        :poll/title title
        :poll/type poll-type
        :poll/discussion discussion-id
        :poll/options (mapv (fn [val] {:db/id (.toString (UUID/randomUUID))
                                       :option/value val}) options)}]
      "newly-created-poll"
      patterns/poll))))

(>defn- poll-belongs-to-discussion?
  "Check if poll belongs to a discussion."
  [poll-id share-hash]
  [:db/id :discussion/share-hash => (? nat-int?)]
  (some?
   (query
    '[:find ?discussion .
      :in $ ?poll-id ?share-hash
      :where [?poll-id :poll/title]
      [?poll-id :poll/discussion ?discussion]
      [?discussion :discussion/share-hash ?poll-id]]
    poll-id share-hash)))

(>defn delete-poll!
  "Delete a poll"
  [poll-id share-hash]
  [:db/id :discussion/share-hash :ret (? map?)]
  (when (poll-belongs-to-discussion? poll-id share-hash)
    @(db/transact [[:db/retractEntity poll-id]])))

(>defn polls
  "Return all polls which reference the discussion from the passed `share-hash`."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/poll)]
  (tools/pull-key-up
   (db/query '[:find [(pull ?poll poll-pattern) ...]
               :in $ ?share-hash poll-pattern
               :where [?discussion :discussion/share-hash ?share-hash]
               [?poll :poll/discussion ?discussion]]
             share-hash patterns/poll)))

(>defn vote!
  "Casts a vote for a certain option.
  Share-hash, poll-id and option-id must be known to prove one is not randomly incrementing values.
  Returns nil if combination is invalid and the transaction otherwise."
  [option-id poll-id share-hash]
  [:db/id :db/id :discussion/share-hash :ret (? map?)]
  (when-let [matching-option
             (db/query
              '[:find ?option .
                :in $ ?option ?poll ?share-hash
                :where [?poll :poll/options ?option]
                [?poll :poll/discussion ?discussion]
                [?discussion :discussion/share-hash ?share-hash]]
              option-id poll-id share-hash)]
    (db/increment-number matching-option :option/votes)))

(defn vote-multiple!
  "Casts a vote for a multiple options.
  Share-hash, poll-id and option-ids must be known to prove one is not randomly incrementing values.
  Returns nil if all combinations are invalid and the transaction with the valid votes otherwise."
  [option-ids poll-id share-hash]
  [(s/coll-of :db/id) :db/id :discussion/share-hash :ret (? map?)]
  (let [matching-options
        (db/query
         '[:find [?options ...]
           :in $ [?options ...] ?poll ?share-hash
           :where [?poll :poll/options ?options]
           [?poll :poll/discussion ?discussion]
           [?discussion :discussion/share-hash ?share-hash]]
         option-ids poll-id share-hash)
        transaction-results (doall (map #(db/increment-number % :option/votes) matching-options))
        clean-results (remove nil? transaction-results)]
    (when (seq clean-results)
      clean-results)))
