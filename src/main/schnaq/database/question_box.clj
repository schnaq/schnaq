(ns schnaq.database.question-box
  (:require [com.fulcrologic.guardrails.core :refer [>defn =>]]
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
