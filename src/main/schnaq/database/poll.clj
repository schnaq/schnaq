(ns schnaq.database.poll
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ? =>]]
            [schnaq.database.main :as db :refer [query fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]))

(>defn new-poll!
  "Create and return a poll entity. Options must be passed as a collection of strings."
  [share-hash title poll-type options hide-results?]
  [:discussion/share-hash :poll/title :poll/type (s/coll-of ::specs/non-blank-string) :poll/hide-results? => (? ::specs/poll)]
  (when (< 0 (count options))
    (db/transact-and-pull-temp
     [{:db/id "newly-created-poll"
       :poll/title title
       :poll/type poll-type
       :poll/discussion [:discussion/share-hash share-hash]
       :poll/options (mapv (fn [val] {:db/id (str (random-uuid))
                                      :option/value val}) options)
       :poll/hide-results? hide-results?}]
     "newly-created-poll"
     patterns/poll)))

(>defn poll-from-discussion
  "Get valid poll from a discussion. Returns `nil` if the poll or discussion
  is invalid."
  [share-hash poll-id]
  [:discussion/share-hash :db/id => (? ::specs/poll)]
  (query
   '[:find (pull ?poll-id pattern) .
     :in $ ?poll-id ?share-hash pattern
     :where [?poll-id :poll/title]
     [?poll-id :poll/discussion ?discussion]
     [?discussion :discussion/share-hash ?share-hash]]
   poll-id share-hash patterns/poll))

(>defn- poll-belongs-to-discussion?
  "Check if poll belongs to a discussion."
  [share-hash poll-id]
  [:discussion/share-hash :db/id => boolean?]
  (some? (poll-from-discussion share-hash poll-id)))

(>defn- edit-poll-options
  "Allow to remove, add and modify options of a poll."
  [share-hash poll-id new-options removed-options edit-options]
  [:discussion/share-hash :db/id (s/coll-of ::specs/non-blank-string) (s/coll-of :db/id)
   (s/coll-of (s/keys :req-un [::id ::value])) => (? ::specs/poll)]
  (when (poll-belongs-to-discussion? share-hash poll-id)
    (let [current-option-ids (set (map :db/id (:poll/options (db/fast-pull poll-id [{:poll/options [:db/id]}]))))
          new-transactions (mapv (fn [option]
                                   {:db/id (str (random-uuid))
                                    :option/value option})
                                 new-options)
          add-transactions (mapv (fn [tx-map]
                                   (vector :db/add poll-id :poll/options (:db/id tx-map)))
                                 new-transactions)
          remove-transactions (mapv (fn [id] (vector :db/retractEntity id))
                                    (filter #(contains? current-option-ids %) removed-options))
          edit-transactions (mapv (fn [{:keys [id value]}]
                                    {:db/id id
                                     :option/value value})
                                  (filter #(contains? current-option-ids (:id %)) edit-options))
          concat-tx (concat new-transactions add-transactions remove-transactions edit-transactions)]
      (when (not-empty concat-tx)
        (->> @(db/transact concat-tx)
             :db-after
             (db/fast-pull poll-id patterns/poll))))))

(>defn delete-poll!
  "Delete a poll"
  [share-hash poll-id]
  [:discussion/share-hash :db/id :ret (? map?)]
  (when (poll-belongs-to-discussion? share-hash poll-id)
    (db/delete-entity! poll-id)))

(>defn polls
  "Return all polls which reference the discussion from the passed `share-hash`."
  [share-hash]
  [:discussion/share-hash :ret (s/coll-of ::specs/poll)]
  (query '[:find [(pull ?poll poll-pattern) ...]
           :in $ ?share-hash poll-pattern
           :where [?discussion :discussion/share-hash ?share-hash]
           [?poll :poll/discussion ?discussion]]
         share-hash patterns/poll))

(>defn vote!
  "Casts a vote for a certain option.
  Share-hash, poll-id and option-id must be known to prove one is not randomly incrementing values.
  Returns nil if combination is invalid and the transaction otherwise."
  [share-hash poll-id option-id]
  [:discussion/share-hash :db/id :db/id :ret (? map?)]
  (when-let [matching-option
             (query
              '[:find ?option .
                :in $ ?option ?poll ?share-hash
                :where [?poll :poll/options ?option]
                [?poll :poll/discussion ?discussion]
                [?discussion :discussion/share-hash ?share-hash]]
              option-id poll-id share-hash)]
    (db/increment-number matching-option :option/votes)))

(defn- match-options
  "Check whether share-hash and poll-id match and return all option-ids that match as well."
  [share-hash poll-id option-ids]
  (query
   '[:find [?options ...]
     :in $ [?options ...] ?poll ?share-hash
     :where [?poll :poll/options ?options]
     [?poll :poll/discussion ?discussion]
     [?discussion :discussion/share-hash ?share-hash]]
   option-ids poll-id share-hash))

(>defn vote-multiple!
  "Casts a vote for a multiple options.
  Share-hash, poll-id and option-ids must be known to prove one is not randomly incrementing values.
  Returns nil if all combinations are invalid and the transaction with the valid votes otherwise."
  [share-hash poll-id option-ids]
  [:discussion/share-hash :db/id (s/coll-of :db/id) :ret (? (s/coll-of map?))]
  (let [matching-options (match-options share-hash poll-id option-ids)
        transaction-results (doall (map #(db/increment-number % :option/votes) matching-options))
        clean-results (remove nil? transaction-results)]
    (when (seq clean-results)
      clean-results)))

(>defn vote-ranking!
  "Check whether the rank distribution is correct. (Not more options than allowed)
  Then votes accordingly. When there are 8 options the first rank gets 8 votes, etc."
  [share-hash poll-id option-id-tuples]
  [:discussion/share-hash :db/id (s/coll-of :db/id) :ret (? (s/coll-of map?))]
  (let [option-num (count (:poll/options (fast-pull poll-id [:poll/options])))]
    (if (>= option-num (count option-id-tuples))
      (let [matching-options (set (match-options share-hash poll-id option-id-tuples))
            ;; Taking the results here directly destroys the order, so we filter in order
            matching-ordered-options (filter matching-options option-id-tuples)]
        (doall
         (for [[option-id increment-num] (partition 2 (interleave matching-ordered-options (range option-num 0 -1)))]
           (db/increment-number option-id :option/votes increment-num))))
      false)))

(>defn toggle-hide-poll-results
  "Toggle if participants can see the poll-results or not."
  [share-hash poll-id hide-results?]
  [:discussion/share-hash :db/id :poll/hide-results? => any?]
  (when (poll-belongs-to-discussion? share-hash poll-id)
    @(db/transact [[:db/add poll-id
                    :poll/hide-results? hide-results?]])))
