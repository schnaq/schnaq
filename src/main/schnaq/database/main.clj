(ns schnaq.database.main
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [datomic.client.api :as d]
            [datomic.api :as datomic]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.config :as config]
            [schnaq.database.models :as models]
            [schnaq.database.specs :as specs]
            [schnaq.test-data :as test-data]
            [schnaq.toolbelt :as toolbelt])
  (:import (java.util UUID Date)))

(def ^:private datomic-info
  (atom {:client nil
         :database-name nil}))

#_(>defn- reset-datomic-client!
    "Sets a new datomic client for transactions."
    [datomic-config]
    [map? :ret any?]
    (swap! datomic-info assoc :client (d/client datomic-config)))

#_(>defn- reset-datomic-db-name!
    "Sets a new database-name for transactions."
    [database-name]
    [string? :ret any?]
    (swap! datomic-info assoc :database-name database-name))

(defn new-connection
  "Connects to the database and returns a connection."
  []
  (datomic/connect config/datomic-uri))

(defn transact
  "Shorthand for transaction."
  [data]
  (datomic/transact (new-connection) data))

(defn query
  "Shorthand to not type out the same first param every time"
  [query-vector & args]
  (apply d/q query-vector (d/db (new-connection)) args))

(>defn delete-database!
  "Delete a database by its name."
  []
  [:ret boolean?]
  (let [{:keys [client database-name]} @datomic-info]
    (d/delete-database
      client
      {:db-name database-name})))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init! config/datomic-uri))
  ([datomic-uri]
   (datomic/create-database datomic-uri)
   (transact models/datomic-schema)))
(comment
  (new-connection)
  (datomic/create-database config/datomic-uri)
  (transact models/datomic-schema)
  (init!)
  )

(defn init-and-seed!
  "Initializing the datomic database and feeding it with test-data.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init-and-seed! {:datomic config/datomic
                    :name config/db-name}))
  ([config]
   (init-and-seed! config test-data/schnaq-test-data))
  ([config test-data]
   (init! config)
   (transact test-data)))

(>defn merge-entity-and-transaction
  "When pulling entity and transaction, merge the results into a single map."
  [[entity transaction]]
  [(s/coll-of map?) :ret map?]
  (merge entity transaction))

;; -----------------------------------------------------------------------------
;; Pull Patterns

(def transaction-pattern
  "Pull transaction information."
  [:db/txInstant])

;; ##### Input functions #####
(defn now [] (Date.))

(>defn clean-db-vals
  "Removes all entries from a map that have a value of nil or empty string."
  [data]
  [associative? :ret associative?]
  (into {} (remove #(or (nil? (second %))
                        (when (= String (type (second %)))
                          (string/blank? (second %))))
                   data)))

(defn fast-pull
  "Pulls any entity with star-syntax and current db."
  ([id]
   (fast-pull id '[*]))
  ([id pattern]
   (d/pull (d/db (new-connection)) pattern id)))

(>defn clean-and-add-to-db!
  "Removes empty strings and nil values from map before transacting it to the
  database. Checks if the specification still matches. If true, transact the
  entity."
  [entity spec]
  [associative? keyword? :ret int?]
  (let [clean-entity (clean-db-vals entity)
        identifier (format "new-entity-%s"
                           (.toString (UUID/randomUUID)))]
    (when (s/valid? spec clean-entity)
      (get-in
        (transact [(assoc clean-entity :db/id identifier)])
        [:tempids identifier]))))

;; -----------------------------------------------------------------------------
;; Feedback functions

(>defn add-feedback!
  "Adds a feedback to the database. Returns the id of the newly added feedback."
  [feedback]
  [::specs/feedback :ret int?]
  (clean-and-add-to-db! feedback ::specs/feedback))

(defn all-feedbacks
  "Return complete feedbacks from database, sorted by descending timestamp."
  []
  (->> (query
         '[:find (pull ?feedback [*]) (pull ?tx transaction-pattern)
           :in $ transaction-pattern
           :where [?feedback :feedback/description _ ?tx]]
         transaction-pattern)
       (map merge-entity-and-transaction)
       (sort-by :db/txInstant toolbelt/ascending)))
