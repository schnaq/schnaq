(ns schnaq.database.feedback-form
  (:require
   [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
   [schnaq.database.main :as db]))

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
