(ns schnaq.database.main
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [datomic.api :as datomic]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.database.models :as models]
            [schnaq.database.specs :as specs]
            [schnaq.test-data :as test-data]
            [schnaq.toolbelt :as toolbelt])
  (:import (java.util UUID Date)))

(def ^:private current-datomic-uri (atom config/datomic-uri))

(defn new-connection
  "Connects to the database and returns a connection."
  []
  (datomic/connect @current-datomic-uri))

(defn transact
  "Shorthand for transaction. Deref the result, if you need to further use it."
  [data]
  (datomic/transact (new-connection) data))

(defn query
  "Shorthand to not type out the same first param every time"
  [query-vector & args]
  (apply datomic/q query-vector (datomic/db (new-connection)) args))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init! config/datomic-uri))
  ([datomic-uri]
   (reset! current-datomic-uri datomic-uri)
   (datomic/create-database datomic-uri)
   (transact models/datomic-schema)))

(defn init-and-seed!
  "Initializing the datomic database and feeding it with test-data.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init-and-seed! config/datomic-uri))
  ([datomic-uri]
   (init-and-seed! datomic-uri test-data/schnaq-test-data))
  ([datomic-uri test-data]
   (init! datomic-uri)
   (transact test-data)))

(>defn merge-entity-and-transaction
  "When pulling entity and transaction, merge the results into a single map."
  [[entity transaction]]
  [(s/coll-of map?) :ret map?]
  (merge entity transaction))

;; TODO update creation of 4 entities with created-at time
;; TODO also update patterns of those entities
;; TODO look for transaction-pattern as well
(comment
  ;; For playing around until we go live with new db
  (new-connection)
  (datomic/create-database config/datomic-uri)
  (transact models/datomic-schema)
  (datomic/delete-database config/datomic-uri)
  (init-and-seed!)
  (datomic/q
    '[:find ?name ?score
      :in $ ?search
      :where [(fulltext $ :statement/content ?search) [[?entity ?name ?tx ?score]]]]
    (datomic/db (datomic/connect config/datomic-uri))
    "dog")
  (query '[:find (pull ?any [*]) (pull ?tx [:db/txInstant])
           :where [?any :statement/content _ ?tx]])
  ;; IMPORT of dev-local-export
  ;; DO NOT CHANGE OR DELETE HERE
  (let [txs (read-string (slurp "db-export.edn"))
        better-txs (toolbelt/pull-key-up txs :db/ident)
        even-better-txs (toolbelt/db-to-ref better-txs)]
    @(transact even-better-txs))
  :end)

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
   (datomic/pull (datomic/db (new-connection)) pattern id))
  ([id pattern db]
   (datomic/pull db pattern id)))

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
        @(transact [(assoc clean-entity :db/id identifier)])
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
       (sort-by :feedback/created-at toolbelt/ascending)))
