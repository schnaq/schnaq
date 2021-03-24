(ns schnaq.database.reaction
  (:require [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.database.user :as user-db]
            [schnaq.meeting.database :refer [transact query]]))

;; ----------------------------------------------------------------------------
;; voting
;; ----------------------------------------------------------------------------

(>defn- vote-on-statement!
  "Up or Downvote a statement"
  [statement-id user-nickname vote-type]
  [number? string? keyword?
   :ret associative?]
  (let [user (user-db/user-by-nickname user-nickname)
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
    (query
      '[:find ?user
        :in $ ?statement
        :where [?user :user/upvotes ?statement]]
      statement-id)))

(>defn downvotes-for-statement
  "Returns the number of downvotes for a statement."
  [statement-id]
  [number? :ret number?]
  (count
    (query
      '[:find ?user
        :in $ ?statement
        :where [?user :user/downvotes ?statement]]
      statement-id)))

(>defn remove-upvote!
  "Removes an upvote of a user."
  [statement-id user-nickname]
  [number? string? :ret associative?]
  (when-let [user (user-db/user-by-nickname user-nickname)]
    (transact [[:db/retract user :user/upvotes statement-id]])))

(>defn remove-downvote!
  "Removes a downvote of a user."
  [statement-id user-nickname]
  [number? string? :ret associative?]
  (when-let [user (user-db/user-by-nickname user-nickname)]
    (transact [[:db/retract user :user/downvotes statement-id]])))

(>defn- generic-reaction-check
  "Checks whether a user already made some reaction."
  [statement-id user-nickname field-name]
  [number? string? keyword? :ret (? number?)]
  (ffirst
    (query
      '[:find ?user
        :in $ ?statement ?nickname ?field-name
        :where [?user :user/nickname ?nickname]
        [?user ?field-name ?statement]]
      statement-id user-nickname field-name)))

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