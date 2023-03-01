(ns schnaq.database.feedback-form
  (:require
   [clojure.spec.alpha :as s]
   [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
   [schnaq.database.main :as db]
   [schnaq.database.patterns :as patterns]
   [schnaq.database.specs :as specs]))

(>defn new-feedback-form!
  "Create a feedback-form and return the id of the new one. Empty form-items are rejected."
  [share-hash form-items]
  [:discussion/share-hash :feedback/items => (? :db/id)]
  (when-not (empty? form-items)
    (:db/id
     (db/transact-and-pull-temp
      [{:db/id "new-feedback-form"
        :feedback/items form-items}
       [:db/add [:discussion/share-hash share-hash] :discussion/feedback "new-feedback-form"]]
      "new-feedback-form"
      [:db/id]))))

(>defn delete-feedback!
  "Deletes the feedback specified."
  [share-hash]
  [:discussion/share-hash => (? associative?)]
  (when-let [feedback-id (:db/id (:discussion/feedback (db/fast-pull [:discussion/share-hash share-hash] '[:discussion/feedback])))]
    @(db/transact [[:db/retractEntity feedback-id]])))

(>defn feedback-items
  "Query and return a feedback-items belonging to a schnaq, if feedback existing and visible."
  [share-hash]
  [:discussion/share-hash => (? :feedback/items)]
  (db/query '[:find [(pull ?items feedback-items-pattern) ...]
              :in $ ?share-hash feedback-items-pattern
              :where [?discussion :discussion/share-hash ?share-hash]
              [?discussion :discussion/feedback ?feedback]
              [?feedback :feedback/visible true]
              [?feedback :feedback/items ?items]]
            share-hash patterns/feedback-item))

(>defn update-feedback-form-items!
  "Updates the feedback form items. Leaves the answers untouched."
  [share-hash form-items visible?]
  [:discussion/share-hash :feedback/items boolean? => (? :db/id)]
  (when-not (empty? form-items)
    (when-let [feedback-id (:discussion/feedback (db/fast-pull [:discussion/share-hash share-hash] patterns/discussion))]
      (let [current-items (feedback-items share-hash)
            new-item-ids (set (remove nil? (map :db/id form-items)))
            items-to-remove (remove #(new-item-ids (:db/id %)) current-items)]
        @(db/transact
          (vec (concat
                (map #(vector :db/retractEntity (:db/id %)) items-to-remove)
                [[:db/add feedback-id :feedback/visible visible?]]
                (map #(vector :db/add feedback-id :feedback/items (if (:db/id %)
                                                                    (:db/id %)
                                                                    (str "item-" (:feedback.item/ordinal %))))
                     form-items)
                ;; IMPORTANT: The % must be second in merge, since we want to preserve existing :db/ids
                (map #(merge {:db/id (str "item-" (:feedback.item/ordinal %))} %) form-items)))))
      feedback-id)))

(>defn add-answers
  "Add new answers to a feedback."
  [share-hash answers]
  [:discussion/share-hash :feedback/answers => boolean?]
  (boolean
   (when (s/valid? :feedback/answers answers)
     (when-let [feedback-id (:discussion/feedback (db/fast-pull [:discussion/share-hash share-hash] patterns/discussion))]
       (let [indexed-answers (map-indexed (fn [idx answer] (merge {:db/id (str "answer-" idx)} answer)) answers)]
         @(db/transact
           (vec
            (concat
             ;; Add the temp-ids to the answer set
             (map #(vector :db/add feedback-id :feedback/answers (:db/id %)) indexed-answers)
             ;; Add the answers themselves
             indexed-answers))))))))

(>defn feedback-form-complete
  "Returns the feedback form including all answers (and their questions)."
  [share-hash]
  [:discussion/share-hash => ::specs/feedback-form]
  (db/query '[:find (pull ?feedback feedback-result-pattern) .
              :in $ ?share-hash feedback-result-pattern
              :where [?discussion :discussion/share-hash ?share-hash]
              [?discussion :discussion/feedback ?feedback]]
            share-hash patterns/feedback-form-results))
