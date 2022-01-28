(ns schnaq.database.survey
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as tools])
  (:import (java.util UUID)))

(>defn new-survey!
  "Create and return a survey entity. Options must be passed as a collection of strings."
  [title survey-type options discussion-id]
  [:survey/title :survey/type (s/coll-of ::specs/non-blank-string) :db/id :ret (? ::specs/survey)]
  (when (< 0 (count options))
    (tools/pull-key-up
     (db/transact-and-pull-temp
      [{:db/id "newly-created-survey"
        :survey/title title
        :survey/type survey-type
        :survey/discussion discussion-id
        :survey/options (mapv (fn [val] {:db/id (.toString (UUID/randomUUID))
                                         :option/value val}) options)}
       {:db/id "newly-created-poll"
        :poll/title title
        :poll/type (if (= :survey.type/single-choice survey-type)
                     :poll.type/single-choice
                     :poll.type/multiple-choice)
        :poll/discussion discussion-id
        :poll/options (mapv (fn [val] {:db/id (.toString (UUID/randomUUID))
                                       :option/value val}) options)}]
      "newly-created-survey"
      patterns/survey))))

(>defn surveys
  "Return all surveys which reference the discussion from the passed `share-hash`."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/survey)]
  (tools/pull-key-up
   (db/query '[:find [(pull ?survey survey-pattern) ...]
               :in $ ?share-hash survey-pattern
               :where [?discussion :discussion/share-hash ?share-hash]
               [?survey :survey/discussion ?discussion]]
             share-hash patterns/survey)))

(>defn vote!
  "Casts a vote for a certain option.
  Share-hash, survey-id and option-id must be known to prove one is not randomly incrementing values.
  Returns nil if combination is invalid and the transaction otherwise."
  [option-id survey-id share-hash]
  [:db/id :db/id :discussion/share-hash :ret (? map?)]
  (when-let [matching-option
             (db/query
              '[:find ?option .
                :in $ ?option ?survey ?share-hash
                :where [?survey :survey/options ?option]
                [?survey :survey/discussion ?discussion]
                [?discussion :discussion/share-hash ?share-hash]]
              option-id survey-id share-hash)]
    (db/increment-number matching-option :option/votes)))

(defn vote-multiple!
  "Casts a vote for a multiple options.
  Share-hash, survey-id and option-ids must be known to prove one is not randomly incrementing values.
  Returns nil if all combinations are invalid and the transaction with the valid votes otherwise."
  [option-ids survey-id share-hash]
  [(s/coll-of :db/id) :db/id :discussion/share-hash :ret (? map?)]
  (let [matching-options
        (db/query
         '[:find [?options ...]
           :in $ [?options ...] ?survey ?share-hash
           :where [?survey :survey/options ?options]
           [?survey :survey/discussion ?discussion]
           [?discussion :discussion/share-hash ?share-hash]]
         option-ids survey-id share-hash)
        transaction-results (doall (map #(db/increment-number % :option/votes) matching-options))
        clean-results (remove nil? transaction-results)]
    (when (seq clean-results)
      clean-results)))
