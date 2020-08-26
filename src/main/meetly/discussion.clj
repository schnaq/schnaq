(ns meetly.discussion
  (:require [clojure.spec.alpha :as s]
            [dialog.discussion.database :as dialog-db]
            [ghostwheel.core :refer [>defn >defn-]]
            [meetly.meeting.database :as db]))

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

(>defn- create-link
  [statement arguments]
  [map? sequential? :ret sequential?]
  (let [children (direct-children (:id statement) arguments)]
    (map (fn [child]
           {:source (:db/id child) :target (:id statement) :type (:type child)})
         children)))

(>defn- create-links
  "Create a link for every argument."
  [statements arguments]
  [sequential? sequential? :ret sequential?]
  (remove empty? (flatten (map #(create-link % arguments) statements))))

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

(>defn- mark-starting-nodes
  "Marks every conclusion-node belonging to a starting-argument in preparation for the graph view."
  [nodes starting-arguments]
  [sequential? sequential? :ret sequential?]
  (let [starting-conclusions (into #{} (map #(-> % :argument/conclusion :db/id) starting-arguments))]
    (map
      (fn [node]
        (assoc
          (if (starting-conclusions (:id node))
            (assoc node :starting-statement? true)
            (assoc node :starting-statement? false))
          :type "statement"))
      nodes)))

(>defn- agenda-node
  "Creates node data for an agenda point."
  [discussion-id meeting-hash]
  [int? string? :ret map?]
  (let [agenda (db/agenda-by-discussion-id discussion-id)
        meeting (db/meeting-by-hash meeting-hash)
        author (db/user (-> meeting :meeting/author :db/id))]
    {:id (:db/id (:agenda/discussion agenda))
     :content (:agenda/title agenda)
     :author (:author/nickname (:user/core-author author))
     :type "agenda"}))

(>defn- agenda-links
  "Creates links from an starting argument to an agenda node."
  [discussion-id starting-arguments]
  [int? sequential? :ret set?]
  (set (map (fn [argument] {:source (-> argument :argument/conclusion :db/id)
                            :target discussion-id
                            :type :argument.type/starting}) starting-arguments)))

(>defn nodes-for-agenda
  "Returns all nodes for a discussion including its agenda."
  [statements starting-arguments discussion-id share-hash]
  [sequential? sequential? int? :meeting/share-hash :ret sequential?]
  (conj (mark-starting-nodes statements starting-arguments)
        (agenda-node discussion-id share-hash)))

(>defn links-for-agenda
  "Creates all links for a discussion with its agenda as root."
  [statements starting-arguments discussion-id]
  [sequential? sequential? int? :ret sequential?]
  (let [arguments (dialog-db/all-arguments-for-discussion discussion-id)]
    (concat
      (create-links statements arguments)
      (agenda-links discussion-id starting-arguments))))