(ns schnaq.discussion
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.user :as user]))

(>defn- premise-ids
  "Return all premise-ids of a single argument."
  [argument]
  [map? :ret (s/coll-of int?)]
  (map :db/id (:argument/premises argument)))

(>defn- undercuts-for-root
  "Find all undercuts, where root-statement is undercut. (Root is set as a premise)"
  [root-id all-arguments]
  [(s/or :id int? :share-hash :discussion/share-hash) sequential? :ret sequential?]
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
  [(s/or :id int? :share-hash :discussion/share-hash) sequential? :ret sequential?]
  (let [arguments-with-root (filter
                              #(= root-id (get-in % [:argument/conclusion :db/id]))
                              all-arguments)
        potential-undercuts (undercuts-for-root root-id all-arguments)
        children (concat arguments-with-root potential-undercuts)
        premises-list (map assoc-type-to-premises children)]
    ;; This works because we do not have premise-groups implemented
    (flatten premises-list)))

(>defn- create-links
  "Create a link for every argument."
  [statements]
  [sequential? :ret sequential?]
  (->> statements
       (filter :statement/parent)
       (map (fn [statement]
              {:from (:db/id statement) :to (-> statement :statement/parent :db/id)
               :type (:statement/type statement)}))))

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
      (let [[next-child & rest-children] descendants]
        (recur (:db/id next-child)
               (concat rest-children (direct-children (:db/id next-child) arguments))
               (inc sub-statements-count)
               (conj authors (or (:statement/author next-child) (:argument/author next-child)))))
      {:sub-statements sub-statements-count
       :authors authors})))

(>defn- create-node
  "Adds a type to the node.
  Checks if the node is a starting statement.
  If the current node is no starting statement checks if the current node is present as a premise in an argument.
  If so add the type of the argument to the node."
  [statement arguments starting-statements]
  [map? sequential? set? :ret map?]
  (let [statement-id (:id statement)
        premise (first (filter #((set (premise-ids %)) statement-id) arguments))]
    (if (contains? starting-statements statement-id)
      (assoc statement :type :argument.type/starting)
      (assoc statement :type (:argument/type premise)))))

(>defn- create-nodes
  "Iterates over every node and marks starting nodes and premise types. Used in the graph view"
  [statements]
  [sequential? :ret sequential?]
  (map #(if (:type %) % (assoc % :type :statement.type/starting)) statements))

(>defn- starting-node
  "Creates node data for a starting point representing the discussion."
  [share-hash]
  [:discussion/share-hash :ret map?]
  (let [discussion (discussion-db/discussion-by-share-hash share-hash)
        author (fast-pull (-> discussion :discussion/author :db/id) user-db/combined-user-pattern)]
    {:id share-hash
     :label (:discussion/title discussion)
     :author (user/display-name author)
     :type :agenda}))

(>defn- starting-links
  "Creates links from an starting statement to the topic node."
  [share-hash starting-statements]
  [:discussion/share-hash sequential? :ret sequential?]
  (map (fn [statement] {:from (:db/id statement)
                        :to share-hash
                        :type :argument.type/starting})
       starting-statements))

(>defn nodes-for-agenda
  "Returns all nodes for a discussion including its agenda."
  [statements share-hash]
  [sequential? :discussion/share-hash :ret sequential?]
  (conj (create-nodes statements)
        (starting-node share-hash)))

(>defn links-for-starting
  "Creates all links for a discussion with its discussion topic as root."
  [starting-statements share-hash]
  [sequential? :discussion/share-hash :ret sequential?]
  (let [statements (discussion-db/all-statements share-hash)]
    (concat
      (create-links statements)
      (starting-links share-hash starting-statements))))

(>defn- update-controversy-map
  "Updates controversy-map with the contents from a single edge."
  [controversy-map edge]
  [map? map? :ret map?]
  (let [sentiment (case (:type edge)
                    :argument.type/attack :negative
                    :argument.type/support :positive
                    :none)]
    (if (= sentiment :none)
      controversy-map
      (update-in controversy-map [(:to edge) sentiment] #(if % (inc %) 1)))))

(>defn- single-controversy-val
  "Calculate a single controversy value in a safe way."
  [controversy-map]
  [map? :ret number?]
  (let [negatives (get controversy-map :negative 0)
        positives (get controversy-map :positive 0)]
    (if (zero? negatives)
      0
      (float
        (* 100
           (/ negatives (+ negatives positives)))))))

(>defn calculate-controversy
  "Calculates controversy values given a set of edges. Returns a hash-map of id -> controversy-value"
  [edges]
  [(s/coll-of map?) :ret map?]
  (reduce
    #(assoc %1 (key %2) (single-controversy-val (val %2)))
    {}
    (reduce update-controversy-map {} edges)))

(>defn- annotate-undercut-premise-meta
  "Annotates undercut-statements with proper meta-information."
  [statements]
  [(s/coll-of ::specs/statement) :ret (s/coll-of ::specs/statement)]
  (map #(assoc % :meta/argument-type :argument.type/undercut) statements))

(>defn premises-undercutting-argument-with-premise-id
  "Return all statements that are used to undercut an argument where `statement-id`
  is used as one of the premises in the undercut argument. Return values are enriched
  with meta-information."
  [statement-id]
  [:db/id :ret (s/coll-of ::specs/statement)]
  (annotate-undercut-premise-meta (discussion-db/statements-undercutting-premise statement-id)))
