(ns meetly.discussion
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]))

(>defn- premise-ids
  "Return all premise-ids of a single argument."
  [argument]
  [map? :ret (s/coll-of int?)]
  (map :db/id (:argument/premises argument)))

(>defn- undercuts-for-root
  "Find all undercuts, where root-statement is undercut. (Root is set as a premise)"
  [root-id all-arguments]
  [int? sequential? :ret sequential?]
  (let [subset-arguments (filter #((set (premise-ids %)) root-id) all-arguments)
        argument-ids (map :db/id subset-arguments)]
    (filter #((set argument-ids) (get-in % [:argument/conclusion :db/id])) all-arguments)))

(>defn- assoc-type-to-premises
  "Assocs a type to a list of premises"
  [argument]
  [map? :ret sequential?]
  (let [premises (:argument/premises argument)]
    (map #(assoc % :type (:argument/type argument)) premises)))

(>defn- direct-children
  "Looks up all direct children of a node. An undercut is considered a child of the premise
  of an argument."
  [root-id all-arguments]
  [int? sequential? :ret sequential?]
  (let [arguments-with-root (filter
                              #(= root-id (get-in % [:argument/conclusion :db/id]))
                              all-arguments)
        potential-undercuts (undercuts-for-root root-id all-arguments)
        children (concat arguments-with-root potential-undercuts)
        premises-list (map assoc-type-to-premises children)]
    (flatten premises-list)))

(>defn create-link
  [statement arguments]
  [map? sequential? :ret sequential?]
  (let [children (direct-children (:id statement) arguments)]
    (map (fn [child]
           {:source (:id statement) :target (:db/id child) :type (:type child)})
         children)))

(>defn sub-discussion-information
  "Returns statistics about the sub-discussion starting with `root-statement-id`.
  Does not watch out for cycles in the graph, only aggregates information for root-statement."
  [root-statement-id arguments]
  [int? sequential? :ret map?]
  (loop [current-root root-statement-id
         descendants (direct-children current-root arguments)
         sub-statements-count 0
         authors #{}]
    (if (seq descendants)
      (let [next-child (first descendants)]
        (recur (:db/id next-child)
               (concat (rest descendants) (direct-children (:db/id next-child) arguments))
               (inc sub-statements-count)
               (conj authors (or (:statement/author next-child) (:argument/author next-child)))))
      {:sub-statements sub-statements-count
       :authors authors})))

(>defn mark-starting-nodes
  "Marks every conclusion-node belonging to a starting-argument in preparation for the graph view."
  [nodes starting-arguments]
  [sequential? sequential? :ret sequential?]
  (let [starting-conclusions (into #{} (map #(-> % :argument/conclusion :db/id) starting-arguments))]
    (map
      (fn [node]
        (if (starting-conclusions (:id node))
          (assoc node :starting-statement? true)
          (assoc node :starting-statement? false)))
      nodes)))
