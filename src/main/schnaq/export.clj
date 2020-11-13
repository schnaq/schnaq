(ns schnaq.export
  (:require [clojure.string :as str]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.meeting.database :as db]
            [schnaq.discussion :as discussion]))

(defn- next-line
  "Builds the next line of a node in txt representation."
  [old-text level node relation]
  (let [relation-symbol (case relation
                          :argument.type/attack "– "
                          :argument.type/undercut "– "
                          :argument.type/support "+ "
                          "")]
    (str old-text "\n" (str/join (repeat level "  ")) relation-symbol (:label node))))

(>defn- nodes-after
  "Delivers all nodes which in the graph of the discussion come after `source-node`.
  Returns nodes as a list of tuples with the type of the link leading to the node
  being the first element.

  E.g. [:argument.type/attack {:db/id …}]"
  [source-node all-statements links]
  [:db/id sequential? sequential? :ret sequential?]
  (let [indexed-nodes (into {} (map #(vector (:id %) %) all-statements))
        bla
        (map #(vector (:type %) (get indexed-nodes (:from %)))
             (filter #(= source-node (:to %)) links))]
    bla))

(>defn generate-text-export
  "Generates a textual representation of the discussion-data."
  [discussion-id share-hash]
  [:db/id string? :ret string?]
  (let [statements (db/all-statements-for-graph discussion-id)
        starting-statements (db/starting-statements discussion-id)
        all-nodes (discussion/nodes-for-agenda statements starting-statements discussion-id share-hash)
        starting-nodes (filter #(= :argument.type/starting (:type %)) all-nodes)
        links (discussion/links-for-agenda all-nodes starting-statements discussion-id)]
    (loop [queue (map #(vector "" %) starting-nodes)
           text ""
           level 0]
      (if (empty? queue)
        ;; We're done here, give the finished text back
        text
        ;; Otherwise either toss the level up marker, or do the recursive algo
        (if (= :level-up-marker (first queue))
          (recur (rest queue) text (dec level))
          (let [[relation first-statement] (first queue)
                updated-text (next-line text level first-statement relation)
                statements-to-add (nodes-after (:id first-statement) all-nodes links)]
            (if (empty? statements-to-add)
              (recur (rest queue) updated-text level)
              (recur (concat statements-to-add [:level-up-marker] (rest queue)) updated-text (inc level)))))))))
