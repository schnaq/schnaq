(ns schnaq.database.reaction
  (:require [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.database.main :refer [transact query]]))

;; ----------------------------------------------------------------------------
;; voting
;; ----------------------------------------------------------------------------

(>defn- vote-on-statement!
  "Up or Downvote a statement."
  [statement-id user-id vote-type]
  [number? :db/id keyword?
   :ret associative?]
  (let [[add-attribute remove-attribute] (if (= vote-type :upvote)
                                           [:statement/upvotes :statement/downvotes]
                                           [:statement/downvotes :statement/upvotes])]
    (transact [[:db/retract statement-id remove-attribute user-id]
               [:db/add statement-id add-attribute user-id]])))

(>defn upvote-statement!
  "Upvotes a statement. Takes a user and a statement-id. The user has to exist, otherwise
  nothing happens."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (vote-on-statement! statement-id user-id :upvote))

(>defn downvote-statement!
  "Downvotes a statement. Takes a user and a statement-id. The user has to exist, otherwise
  nothing happens."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (vote-on-statement! statement-id user-id :downvote))

(>defn upvotes-for-statement
  "Returns the number of upvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (count
    (query
      '[:find ?user
        :in $ ?statement
        :where [?statement :statement/upvotes ?user]]
      statement-id)))

(>defn downvotes-for-statement
  "Returns the number of downvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (count
    (query
      '[:find ?user
        :in $ ?statement
        :where [?statement :statement/downvotes ?user]]
      statement-id)))

(>defn remove-upvote!
  "Removes an upvote of a user."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (transact [[:db/retract statement-id :statement/upvotes user-id]]))

(>defn remove-downvote!
  "Removes a downvote of a user."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (transact [[:db/retract statement-id :statement/downvotes user-id]]))

(>defn- generic-reaction-check
  "Checks whether a user already made some reaction."
  [statement-id user-id field-name]
  [number? :db/id keyword? :ret (? number?)]
  (ffirst
    (query
      '[:find ?statement
        :in $ ?statement ?user ?field-name
        :where [?statement ?field-name ?user]]
      statement-id user-id field-name)))

(>defn did-user-upvote-statement
  "Check whether a user already upvoted a statement."
  [statement-id user-id]
  [number? :db/id :ret (? number?)]
  (generic-reaction-check statement-id user-id :statement/upvotes))

(>defn did-user-downvote-statement
  "Check whether a user already downvoted a statement."
  [statement-id user-id]
  [number? :db/id :ret (? number?)]
  (generic-reaction-check statement-id user-id :statement/downvotes))