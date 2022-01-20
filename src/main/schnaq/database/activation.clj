(ns schnaq.database.activation
  (:require [com.fulcrologic.guardrails.core :refer [>defn >defn- ?]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as tools]))

(>defn- new-activation!
  "Create a new activation for a discussion. 
   This should only be called via start-activation! to avoid duplicates."
  [share-hash]
  [:discussion/share-hash :ret (? ::specs/activation)]
  (let [temp-id "new-activation"]
    (tools/pull-key-up
     (db/transact-and-pull-temp
      [{:db/id temp-id
        :activation/count 0
        :activation/discussion [:discussion/share-hash share-hash]}]
      temp-id patterns/activation))))

(>defn activation-by-share-hash
  "Get the activation for a discussion by share-hash."
  [share-hash]
  [:discussion/share-hash :ret ::specs/activation]
  (first
   (tools/pull-key-up
    (db/query '[:find [(pull ?activation activation-pattern)]
                :in $ ?share-hash activation-pattern
                :where [?discussion :discussion/share-hash ?share-hash]
                [?activation :activation/discussion ?discussion]]
              share-hash patterns/activation))))

(>defn start-activation!
  "Starts a new activation if none has already been created"
  [share-hash]
  [:discussion/share-hash :ret (? ::specs/activation)]
  (if-let [activation (activation-by-share-hash share-hash)]
    activation
    (new-activation! share-hash)))

(>defn reset-activation!
  "Reset activation by share hash and return activation entity"
  [share-hash]
  [:discussion/share-hash :ret ::specs/activation]
  (let [activation-id (:db/id (activation-by-share-hash share-hash))]
    (db/transact [[:db/add activation-id :activation/count 0]])
    (db/fast-pull activation-id patterns/activation)))

(>defn increase-activation!
  "Increase activation by share hash and return activation entity"
  [share-hash]
  [:discussion/share-hash :ret ::specs/activation]
  (let [activation-id (:db/id (activation-by-share-hash share-hash))]
    (db/increment-number activation-id :activation/count)
    (db/fast-pull activation-id patterns/activation)))

