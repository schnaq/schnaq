(ns schnaq.database.xforms
  "Xforms used by datomic pull patterns to transform results.")

(defn maps->ids
  "Takes a collection of maps and extracts the values of all db/ids.
  Example:
  [{:db/id 123} {:db/id 321 :foo/bar :baz}]
  =>
  [123 321]"
  [result-coll]
  (not-empty (map :db/id result-coll)))

(defn pull-up-db-ident
  "Receives an entity and returns the :db/ident inside it."
  [entity]
  (:db/ident entity))

(defn pull-up-db-id
  "Receives an entity and returns the :db/id inside it."
  [entity]
  (:db/id entity))

(defn pull-up-ident-coll
  "Receives a collection of db/ident entities and pulls them up."
  [entity-coll]
  (map :db/ident entity-coll))
