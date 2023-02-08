(ns schnaq.database.analytics
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn >defn-]]
            [schnaq.database.activation :as activation-db]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.poll :as poll-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.shared-toolbelt :refer [deep-merge-with]])
  (:import (java.text SimpleDateFormat)
           (java.time Instant)
           (java.util Date)))

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

(>defn users-created-since
  "Return all users created since given days."
  [since]
  [:statistics/since => :statistics/users]
  (main-db/query
   '[:find [(pull ?users pattern) ...]
     :in $ pattern ?since
     :where [?users :user.registered/email _ ?tx]
     [?tx :db/txInstant ?start-date]
     [(< ?since ?start-date)]]
   patterns/private-user (Date/from since)))

(>defn number-or-registered-users
  "Returns the number of registered users in the database."
  ([]
   [:ret :statistics/registered-users-num]
   (number-of-entities-since :user.registered/display-name))
  ([since]
   [:statistics/since :ret :statistics/registered-users-num]
   (number-of-entities-since :user.registered/display-name since)))

(>defn number-of-pro-users
  "Returns the number of pro users in the database."
  ([]
   [:ret :statistics/registered-users-num]
   (number-of-entities-with-value-since :user.registered/roles :role/pro))
  ([since]
   [:statistics/since :ret :statistics/registered-users-num]
   (number-of-entities-with-value-since
    :user.registered/roles :role/pro since)))

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

(defn- percentile-of
  "Returns the first element from a percentile distribution in a array.
  E.g. (percentile-of [1 4 6 8] 75)
       => 8"
  [collection percentile]
  (let [n (int (Math/ceil (* (/ percentile 100) (count collection))))
        n' (if (zero? n) n (dec n))]
    (if (seq collection)
      (nth collection n')
      0)))

(>defn statistical-statement-num-data
  "Returns the median of statements per discussion."
  ([]
   [:ret map?]
   (statistical-statement-num-data max-time-back))
  ([since]
   [:statistics/since :ret map?]
   (let [statement-data
         (main-db/query
          '[:find ?discussion (count ?statements)
            :in $ ?since
            :where [?discussion :discussion/title _ ?tx]
            [?tx :db/txInstant ?start-date]
            [(< ?since ?start-date)]
            [?statements :statement/discussions ?discussion]]
          (Date/from since))
         sorted-data (sort (map second statement-data))]
     {:25-percentile (percentile-of sorted-data 25)
      :50-percentile (percentile-of sorted-data 50)
      :75-percentile (percentile-of sorted-data 65)
      :90-percentile (percentile-of sorted-data 90)
      :95-percentile (percentile-of sorted-data 95)})))

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
  ([]
   [:ret :statistics/statement-length-stats]
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
   [:statistics/since :ret :statistics/labels-stats]
   {:check (number-of-entities-with-value-since :statement/labels ":check" since)
    :question (number-of-entities-with-value-since :statement/labels ":question" since)
    :times (number-of-entities-with-value-since :statement/labels ":times" since)
    :ghost (number-of-entities-with-value-since :statement/labels ":ghost" since)
    :calendar-alt (number-of-entities-with-value-since :statement/labels ":calendar-alt" since)
    :arrow-right (number-of-entities-with-value-since :statement/labels ":arrow-right" since)
    :comment (number-of-entities-with-value-since :statement/labels ":comment" since)}))

(defn schnaq-usage
  ([]
   [:ret :statistics/usage]
   (schnaq-usage max-time-back))
  ([since]
   [:statistics/since :ret :statistics/usage]
   (->>
    (main-db/query
     '[:find ?entities (pull ?values [:db/ident])
       :in $ ?since
       :where [?entities :surveys.using-schnaq-for/topics ?values ?tx]
       [?tx :db/txInstant ?start-date]
       [(< ?since ?start-date)]]
     (Date/from since))
    (group-by second)
    (map #(vector (:db/ident (first %)) (count (second %)))))))

;; -----------------------------------------------------------------------------
;; Aggregate analytics for a group of users, e.g. matched by their mail addresses

(>defn- total-upvotes
  "Count and sum up all upvotes."
  [statements]
  [(s/coll-of ::specs/statement) => int?]
  (->> statements
       (map :statement/upvotes)
       (map count)
       (apply +)))

(>defn- total-downvotes
  "Count and sum up all downvotes."
  [statements]
  [(s/coll-of ::specs/statement) => int?]
  (->> statements
       (map :statement/downvotes)
       (map count)
       (apply +)))

(>defn- all-activations
  "Returns all activations for a list of share-hashes."
  [share-hashes]
  [(s/coll-of :discussion/share-hash) => (s/coll-of ::specs/activation)]
  (remove nil? (map activation-db/activation-by-share-hash share-hashes)))

(>defn- total-activation-count
  "Count and sum up all button presses in an activation."
  [activations]
  [(s/coll-of map?) => int?]
  (->> activations
       (map :activation/count)
       (apply +)))

(>defn- count-unique-visitors
  "Count all device ids for a list of discussions."
  [discussions]
  [(s/coll-of ::specs/discussion) => int?]
  (->> discussions
       (map :discussion/device-ids)
       (map count)
       (apply +)))

(>defn- count-total-poll-votes
  "Count all votes casted in a list of polls."
  [polls]
  [(s/coll-of ::specs/poll) => int?]
  (->> polls
       (map :poll/options)
       flatten
       (map :option/votes)
       (apply +)))

(>defn- count-words-in-local-wordclouds
  "Count all words in local wordclouds."
  [local-wordclouds]
  [(s/coll-of map?) => int?]
  (->> local-wordclouds
       (map :wordcloud/words)
       flatten
       (filter number?)
       (apply +)))

(>defn- stats-for-user
  "Returns aggregated statistics for a single user."
  [{:user.registered/keys [keycloak-id]}]
  [::specs/registered-user => map?]
  (let [discussions (discussion-db/discussions-from-user keycloak-id)
        share-hashes (map :discussion/share-hash discussions)
        statements (flatten (map discussion-db/all-statements share-hashes))
        activations (flatten (all-activations share-hashes))
        polls (flatten (map poll-db/polls share-hashes))
        wordclouds (map wordcloud-db/wordcloud-by-share-hash share-hashes)
        local-wordclouds (flatten (map wordcloud-db/local-wordclouds share-hashes))]
    {:discussions {:total (count discussions)
                   :statements (count statements)
                   :upvotes (total-upvotes statements)
                   :downvotes (total-downvotes statements)
                   :visitors (count-unique-visitors discussions)}
     :activations {:total (count activations)
                   :total-count (total-activation-count activations)}
     :polls {:total (count polls)
             :total-votes (count-total-poll-votes polls)}
     :wordclouds {:total (count wordclouds)}
     :local-wordclouds {:total (count local-wordclouds)
                        :words (count-words-in-local-wordclouds local-wordclouds)}}))

(defn statistics-for-users-by-email-patterns
  "Aggregate statistics for a list of users found by their email patterns.
   
   Example:
   `(statistics-for-users-by-email-patterns [#\".*@schnaq\\.com\" #\".*@hhu\\.de\"])`"
  [patterns]
  (->> patterns
       (map user-db/users-filter-by-regex-on-email)
       flatten
       (map stats-for-user)
       (apply deep-merge-with +)))
