(ns schnaq.database.hub
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main
             :refer [transact fast-pull query merge-entity-and-transaction]
             :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (java.util Date)))

(def ^:private hub-essential-info-pattern
  [:db/id
   :hub/name
   :hub/keycloak-name])

(def ^:private hub-pattern
  [:db/id
   :hub/name
   :hub/keycloak-name
   {:hub/schnaqs discussion-db/discussion-pattern}])

(>defn- all-schnaqs-for-hub
  "Return all schnaqs belonging to a hub. Includes the tx."
  [hub-id]
  [:db/id :ret any?]
  (as->
    (query
      '[:find (pull ?discussions discussion-pattern) (pull ?tx transaction-pattern)
        :in $ ?hub discussion-pattern transaction-pattern
        :where [?hub :hub/schnaqs ?discussions]
        [?discussions :discussion/title _ ?tx]]
      hub-id discussion-db/discussion-pattern-minimal main-db/transaction-pattern)
    result
    (toolbelt/pull-key-up result :db/ident)
    (map merge-entity-and-transaction result)))

(defn- pull-hub
  "Pull a hub from the database and include all txs pull db/ident up."
  ([hub-query]
   (pull-hub hub-query hub-essential-info-pattern))
  ([hub-query pattern]
   (pull-hub hub-query pattern nil))
  ([hub-query pattern db]
   (let [hub (if db
               (main-db/fast-pull hub-query pattern db)
               (main-db/fast-pull hub-query pattern))]
     (when (:db/id hub)
       (assoc hub :hub/schnaqs (all-schnaqs-for-hub (:db/id hub)))))))

(>defn create-hub
  "Create a hub and reference it to the keycloak-name."
  [hub-name keycloak-name]
  [:hub/name :hub/keycloak-name :ret ::specs/hub]
  (let [tx @(transact [{:db/id "temp"
                        :hub/name hub-name
                        :hub/keycloak-name keycloak-name
                        :hub/created-at (Date.)}])
        new-hub (get-in tx [:tempids "temp"])
        new-db (:db-after tx)]
    (log/info "Created hub" new-hub)
    (pull-hub new-hub hub-essential-info-pattern new-db)))

(>defn create-hubs-if-not-existing
  "Create all hubs that are not yet existent. Returns the input, when no exception was caused."
  [keycloak-names]
  [(s/coll-of :hub/keycloak-name) :ret any?]
  (let [non-existent-keycloak-names
        (remove #(fast-pull [:hub/keycloak-name %] [:hub/keycloak-name]) keycloak-names)
        transaction (mapv #(hash-map :hub/keycloak-name %
                                     :hub/name %
                                     :hub/created-at (Date.))
                          non-existent-keycloak-names)]
    (transact transaction)
    keycloak-names))

(>defn add-discussions-to-hub
  [hub-id discussion-ids]
  [:db/id (s/coll-of :db/id) :ret ::specs/hub]
  (transact (mapv #(vector :db/add hub-id :hub/schnaqs %) discussion-ids))
  (log/info "Added schnaqs with ids" discussion-ids "to hub" hub-id)
  (pull-hub hub-id))

(>defn remove-discussion-from-hub
  [hub-id discussion-id]
  [:db/id :db/id :ret ::specs/hub]
  (transact [[:db/retract hub-id :hub/schnaqs discussion-id]])
  (log/info "Removed schnaq" discussion-id "from hub" hub-id)
  (pull-hub hub-id))

(>defn hub-by-keycloak-name
  "Return a hub by the reference in keycloak."
  [keycloak-name]
  [string? :ret ::specs/hub]
  (pull-hub [:hub/keycloak-name keycloak-name]))

(>defn hubs-by-keycloak-names
  "Takes a list of keycloak-names and returns the hub entities."
  [keycloak-names]
  [(s/coll-of string?) :ret (s/coll-of ::specs/hub)]
  (toolbelt/pull-key-up
    (->> (main-db/query
           '[:find (pull ?hub hub-pattern) (pull ?tx transaction-pattern)
             :in $ [?hub-names ...] hub-pattern transaction-pattern
             :where [?hub :hub/keycloak-name ?hub-names ?tx]]
           keycloak-names hub-pattern main-db/transaction-pattern)
         (map main-db/merge-entity-and-transaction))
    :db/ident))

(>defn change-hub-name
  "Change a hub's name."
  [keycloak-name new-name]
  [string? string? :ret ::specs/hub]
  (let [new-db (:db-after
                 @(transact [[:db/add [:hub/keycloak-name keycloak-name]
                              :hub/name new-name]]))]
    (toolbelt/pull-key-up
      (fast-pull [:hub/keycloak-name keycloak-name] hub-pattern new-db)
      :db/ident)))
