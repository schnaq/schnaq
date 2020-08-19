(ns meetly.discussion
  (:require [dialog.discussion.database :as dialog-db]
            [ghostwheel.core :refer [>defn >defn-]]))

(>defn- descendant-undercuts
  "Finds all undercuts to a set of arguments."
  [argument-subset all-arguments]
  [sequential? sequential? :ret sequential?]
  (let [ids (map :db/id argument-subset)]
    (filter #((set ids) (get-in % [:argument/conclusion :db/id])) all-arguments)))

(>defn- direct-children
  "Looks up all direct children of a node. The edge itself is considered a child as well
  to accommodate undercuts. The root itself can be an edge as well."
  [root-id all-arguments]
  [int? sequential? :ret sequential?]
  (let [arguments-with-root (filter
                              #(= root-id (get-in % [:argument/conclusion :db/id]))
                              all-arguments)
        potential-undercuts (descendant-undercuts arguments-with-root all-arguments)
        children (concat arguments-with-root potential-undercuts)
        premises-list (map :argument/premises children)]
    (flatten premises-list)))

(>defn sub-discussion-information
  "Returns statistics about the sub-discussion starting with `root-statement-id`.
  Does not watch out for cycles in the graph."
  [root-statement-id discussion-id]
  [int? int? :ret map?]
  (let [all-arguments (dialog-db/all-arguments-for-discussion discussion-id)]
    (loop [current-root root-statement-id
           descendants (direct-children current-root all-arguments)
           sub-statements-count 0
           authors #{}]
      (if (seq descendants)
        (let [next-child (first descendants)]
          (recur (:db/id next-child)
                 (concat (rest descendants) (direct-children (:db/id next-child) all-arguments))
                 (inc sub-statements-count)
                 (conj authors (or (:statement/author next-child) (:argument/author next-child)))))
        {:sub-statements sub-statements-count
         :authors authors}))))