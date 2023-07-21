(ns schnaq.database.question-box
  (:require [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]))

(>defn create-qa-box!
  "Create an empty question box with an optional label if passed."
  ([share-hash]
   [:discussion/share-hash => ::specs/qa-box]
   (create-qa-box! share-hash true nil))
  ([share-hash visible?]
   [:discussion/share-hash :qa-box/visible => ::specs/qa-box]
   (create-qa-box! share-hash visible? nil))
  ([share-hash visible? label]
   [:discussion/share-hash :qa-box/visible :qa-box/label => ::specs/qa-box]
   (let [entity-id "new-qa-box"
         prepared-tx [[:db/add entity-id :qa-box/visible visible?]
                      [:db/add [:discussion/share-hash share-hash] :discussion/qa-boxes entity-id]]]
     (db/transact-and-pull-temp (if label
                                  (conj prepared-tx [:db/add entity-id :qa-box/label label])
                                  prepared-tx)
                                entity-id
                                patterns/qa-box))))

(>defn delete-qa-box!
  "Delete a question box by its entity id."
  [entity-id]
  [:db/id => future?]
  (db/transact [[:db/retractEntity entity-id]]))

(>defn update-qa-box
  "Change visibility and label of the qa-box."
  [entity-id visible? label]
  [:db/id :qa-box/visible :qa-box/label => ::specs/qa-box]
  (let [prepared-tx [[:db/add entity-id :qa-box/visible visible?]]
        tx @(db/transact (if label
                           (conj prepared-tx [:db/add entity-id :qa-box/label label])
                           prepared-tx))]
    (->> tx
         :db-after
         (db/fast-pull entity-id patterns/qa-box))))


(>defn add-question
  "Adds a single new question to the question box."
  [qa-box-id question]
  [:db/id ::specs/non-blank-string => (? ::specs/question)]
  (when (not-empty question)
    (let [question-id "new-question"]
      (db/transact-and-pull-temp [[:db/add question-id :qa-box.question/value question]
                                  [:db/add qa-box-id :qa-box/questions question-id]]
                                 question-id
                                 patterns/question))))
