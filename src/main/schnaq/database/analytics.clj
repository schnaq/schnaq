(ns schnaq.database.analytics
  (:require [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.database.main :as main-db]
            [schnaq.database.patterns :as patterns])
  (:import (java.util Date)
           (java.time Instant)
           (java.text SimpleDateFormat)))

(def ^:private max-time-back Instant/EPOCH)

(>defn- number-of-entities-since
  "Returns the number of entities in the db since some timestamp. Default is all."
  ([attribute]
   [keyword? :ret int?]
   (number-of-entities-since attribute max-time-back))
  ([attribute since]
   [keyword? inst? :ret int?]
   (or
    (main-db/query
     '[:find (count ?entities) .
       :in $ ?since ?attribute
       :where [?entities ?attribute _ ?tx]
       [?tx :db/txInstant ?start-date]
       [(< ?since ?start-date)]]
     (Date/from since) attribute)
    0)))

(>defn- number-of-entities-with-value-since
  "Returns the number of entities in the db since some timestamp. Default is all."
  ([attribute value]
   [keyword? any? :ret int?]
   (number-of-entities-with-value-since attribute value max-time-back))
  ([attribute value since]
   [keyword? any? inst? :ret int?]
   (or
    (main-db/query
     '[:find (count ?entities) .
       :in $ ?since ?attribute ?value
       :where [?entities ?attribute ?value ?tx]
       [?tx :db/txInstant ?start-date]
       [(< ?since ?start-date)]]
     (Date/from since) attribute value)
    0)))

(>defn number-of-discussions
  "Returns the number of meetings. Optionally takes a date since when this counts."
  ([]
   [:ret :statistics/discussions-sum]
   (number-of-discussions max-time-back))
  ([since]
   [:statistics/since :ret :statistics/discussions-sum]
   (or
    (main-db/query
     '[:find (count ?discussions) .
       :in $ ?since
       :where [?discussions :discussion/created-at ?timestamp]
       (not-join [?discussions]
                 [?discussions :discussion/states :discussion.state/deleted])
       [(< ?since ?timestamp)]]
     (Date/from since))
    0)))

(>defn number-of-usernames
  "Returns the number of different usernames in the database."
  ([]
   [:ret :statistics/usernames-sum]
   (number-of-entities-since :user/nickname))
  ([since]
   [:statistics/since :ret :statistics/usernames-sum]
   (number-of-entities-since :user/nickname since)))

(>defn number-or-registered-users
  "Returns the number of registered users in the database."
  ([]
   [:ret :statistics/registered-users-num]
   (number-of-entities-since :user.registered/display-name))
  ([since]
   [:statistics/since :ret :statistics/registered-users-num]
   (number-of-entities-since :user.registered/display-name since)))

(defn- date-to-day
  "Get only the date, without time from java.util.date"
  [date]
  (let [df (SimpleDateFormat. "YYYY-'W'ww")]
    (.format df date)))

(defn- statement-num-by-week
  "Counts the frequencies of statements by week."
  [statements]
  (frequencies
   (map #(date-to-day (:statement/created-at %)) statements)))

(>defn number-of-statements
  "Returns the number of different statements in the database."
  ([]
   [:ret :statistics/statements-num]
   (number-of-statements max-time-back))
  ([since]
   [:statistics/since :ret :statistics/statements-num]
   (let [all-statements
         (main-db/query
          '[:find [(pull ?statements [:statement/created-at]) ...]
            :in $ ?since
            :where
            ;; Make sure the discussion is not deleted where the statements are from
            (not [?discussions :discussion/states :discussion.state/deleted])
            [?statements :statement/discussions ?discussions]
            ;; Make sure statements are not deleted
            (not [?statements :statement/deleted? true])
            [?statements :statement/created-at ?timestamp]
            [(< ?since ?timestamp)]]
          (Date/from since))]
     {:overall (count all-statements)
      :series (statement-num-by-week all-statements)})))

(>defn average-number-of-statements
  "Returns the average number of statements per discussion."
  ([]
   [:ret :statistics/average-statements-num]
   (average-number-of-statements max-time-back))
  ([since]
   [:statistics/since :ret :statistics/average-statements-num]
   (let [discussions (number-of-discussions since)
         statements (:overall (number-of-statements since))]
     (if (zero? discussions)
       0
       (/ statements discussions)))))

(>defn number-of-active-discussion-users
  "Returns the number of active (anonymous or registered) users (With at least one statement)."
  ([]
   [:ret :statistics/active-users-num]
   (number-of-active-discussion-users max-time-back))
  ([since]
   [:statistics/since :ret :statistics/active-users-num]
   (let [active-authors
         (main-db/query
          '[:find [(pull ?authors public-user-pattern) ...]
            :in $ ?since public-user-pattern
            :where [?statements :statement/author ?authors ?tx]
            [?tx :db/txInstant ?start-date]
            [(< ?since ?start-date)]]
          (Date/from since) patterns/public-user)
         registered-authors (filter :user.registered/display-name active-authors)
         anonymous-authors (filter :user/nickname active-authors)]
     {:overall (count active-authors)
      :overall/registered (count registered-authors)
      :overall/anonymous (count anonymous-authors)})))

(>defn statement-length-stats
  "Returns a map of stats about statement-length."
  ([] [:ret :statistics/statement-length-stats]
   (statement-length-stats max-time-back))
  ([since]
   [:statistics/since :ret :statistics/statement-length-stats]
   (let [sorted-contents (sort-by count
                                  (main-db/query
                                   '[:find [?contents ...]
                                     :in $ ?since
                                     :where [?statement :statement/content ?contents]
                                     [?statement :statement/created-at ?timestamp]
                                     [(< ?since ?timestamp)]]
                                   (Date/from since)))
         content-count (count sorted-contents)
         max-length (count (last sorted-contents))
         min-length (count (first sorted-contents))
         average-length (if (zero? content-count) 0 (float (/ (reduce #(+ %1 (count %2)) 0 sorted-contents) content-count)))
         median-length (if (zero? content-count) 0 (count (nth sorted-contents (quot content-count 2))))]
     {:max max-length
      :min min-length
      :average average-length
      :median median-length})))

(>defn statement-type-stats
  "Returns the number of attacks, supports and neutrals since a certain timestamp."
  ([]
   [:ret :statistics/statement-type-stats]
   (statement-type-stats max-time-back))
  ([since]
   [:statistics/since :ret :statistics/statement-type-stats]
   {:supports (number-of-entities-with-value-since :statement/type :statement.type/support since)
    :attacks (number-of-entities-with-value-since :statement/type :statement.type/attack since)
    :neutrals (number-of-entities-with-value-since :statement/type :statement.type/neutral since)}))

(>defn labels-stats
  "Returns the number of attacks, supports and neutrals since a certain timestamp."
  ([]
   [:ret :statistics/statement-type-stats]
   (labels-stats max-time-back))
  ([since]
   [:statistics/since :ret :statistics/statement-type-stats]
   {:check (number-of-entities-with-value-since :statement/labels ":check" since)
    :question (number-of-entities-with-value-since :statement/labels ":question" since)
    :times (number-of-entities-with-value-since :statement/labels ":times" since)
    :ghost (number-of-entities-with-value-since :statement/labels ":ghost" since)
    :calendar-alt (number-of-entities-with-value-since :statement/labels ":calendar-alt" since)
    :arrow-right (number-of-entities-with-value-since :statement/labels ":arrow-right" since)
    :comment (number-of-entities-with-value-since :statement/labels ":comment" since)}))
