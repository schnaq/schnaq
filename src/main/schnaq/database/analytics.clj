(ns schnaq.database.analytics
  (:require [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.meeting.database :as main-db])
  (:import (java.util Date)
           (java.time Instant)))

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
       (main-db/query
         '[:find (count ?entities)
           :in $ ?since ?attribute
           :where [?entities ?attribute _ ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (Date/from since) attribute))
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
       (main-db/query
         '[:find (count ?entities)
           :in $ ?since ?attribute ?value
           :where [?entities ?attribute ?value ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (Date/from since) attribute value))
     0)))

(defn number-of-discussions
  "Returns the number of meetings. Optionally takes a date since when this counts."
  ([]
   (number-of-discussions max-time-back))
  ([since]
   (or
     (ffirst
       (main-db/query
         '[:find (count ?discussions)
           :in $ ?since
           :where [?discussions :discussion/title _ ?tx]
           (not-join [?discussions]
                     [?discussions :discussion/states :discussion.state/deleted])
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (Date/from since)))
     0)))

(defn number-of-usernames
  "Returns the number of different usernames in the database."
  ([] (number-of-entities-since :user/nickname))
  ([since] (number-of-entities-since :user/nickname since)))

(defn number-of-statements
  "Returns the number of different usernames in the database."
  ([]
   (number-of-statements max-time-back))
  ([since]
   (or
     (ffirst
       (main-db/query
         '[:find (count ?statements)
           :in $ ?since
           :where
           ;; Make sure the discussion is not deleted where the statements are from
           (not [?discussions :discussion/states :discussion.state/deleted])
           [?arguments :argument/discussions ?discussions]
           (or-join [?discussions ?arguments ?statements]
                    [?arguments :argument/premises ?statements]
                    [?arguments :argument/conclusion ?statements]
                    [?discussions :discussion/starting-statements ?statements])
           ;; Make sure statements are not deleted
           (not [?statements :statement/deleted? true])
           [?statements :statement/content _ ?tx]
           [?tx :db/txInstant ?start-date]
           [(< ?since ?start-date)]]
         (Date/from since)))
     0)))

(>defn average-number-of-statements
  "Returns the average number of statements per discussion."
  ([]
   [:ret number?]
   (average-number-of-statements max-time-back))
  ([since]
   [inst? :ret number?]
   (let [discussions (number-of-discussions since)
         statements (number-of-entities-since :statement/content since)]
     (if (zero? discussions)
       0
       (/ statements discussions)))))

(>defn active-discussion-authors
  "Returns all authors active in a discussion during a period since the provided
  timestamp."
  [since]
  [inst? :ret sequential?]
  (flatten
    (main-db/query
      '[:find ?authors
        :in $ ?since
        :where [?statements :statement/author ?authors ?tx]
        [?tx :db/txInstant ?start-date]
        [(< ?since ?start-date)]]
      (Date/from since))))

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
   (let [sorted-contents (->>
                           (main-db/query
                             '[:find ?contents
                               :in $ ?since
                               :where [_ :statement/content ?contents ?tx]
                               [?tx :db/txInstant ?add-date]
                               [(< ?since ?add-date)]]
                             (Date/from since))
                           flatten
                           (sort-by count))
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
