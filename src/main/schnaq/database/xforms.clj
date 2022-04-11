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
