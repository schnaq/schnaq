(ns schnaq.database.reaction
  (:require [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.meeting.database :refer [transact query]]))

;; ----------------------------------------------------------------------------
;; voting
;; ----------------------------------------------------------------------------

;; TODO delete when reading is done from the new store already
(>defn- vote-on-statement!
  "Up or Downvote a statement
  NOTE: We write to two fields, until the votes are migrated in production. At this point the :user/upvote
  and :user/downvote variant can be removed. From that point on pass a user-id or a lookup ref."
  [statement-id user-id vote-type]
  [number? :db/id keyword?
   :ret associative?]
  (let [[add-field remove-field] (if (= vote-type :upvote)
                                   [:user/upvotes :user/downvotes]
                                   [:user/downvotes :user/upvotes])
        [add-field-new remove-field-new] (if (= vote-type :upvote)
                                           [:statement/upvotes :statement/downvotes]
                                           [:statement/downvotes :statement/upvotes])]
    (transact [[:db/retract user-id remove-field statement-id]
               [:db/add user-id add-field statement-id]
               [:db/retract statement-id remove-field-new user-id]
               [:db/add statement-id add-field-new user-id]])))

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

;; TODO delete when reading is done from the new store already
(>defn remove-upvote!
  "Removes an upvote of a user."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (transact [[:db/retract user-id :user/upvotes statement-id]
             [:db/retract statement-id :statement/upvotes user-id]]))

;; TODO delete when reading is done from the new store already
(>defn remove-downvote!
  "Removes a downvote of a user."
  [statement-id user-id]
  [number? :db/id :ret associative?]
  (transact [[:db/retract user-id :user/downvotes statement-id]
             [:db/retract statement-id :statement/downvotes user-id]]))

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