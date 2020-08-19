(ns meetly.discussion
  (:require [datomic.client.api :as d]
            [dialog.discussion.database :as dialog-db]
            [ghostwheel.core :refer [>defn >defn-]]
            [meetly.meeting.database :as db]))

(>defn- direct-children
  "Looks up all direct children of a node. The edge itself is considered a child as well
  to accommodate undercuts. The root itself can be an edge as well."
  [root-id all-arguments]
  [int? sequential? :ret sequential?]
  (let [arguments-with-root (filter
                              (fn [coll] (or (= root-id (:db/id coll))
                                             (= root-id (get-in coll [:argument/conclusion :db/id]))))
                              all-arguments)
        premises-list (map :argument/premises arguments-with-root)]
    (println (flatten premises-list))
    (map :db/id (flatten premises-list))))

(>defn sub-discussion-information
  "Returns statistics about the sub-discussion starting with `root-statement-id`.
  Does not watch out for cycles in the graph."
  [root-statement-id discussion-id]
  [int? int? :ret map?]
  (let [all-arguments (dialog-db/all-arguments-for-discussion discussion-id)]
    (loop [current-root root-statement-id
           descendants (direct-children current-root all-arguments)
           sub-statements-count 0]
      (if (seq descendants)
        (recur (first descendants)
               (concat (rest descendants) (direct-children (first descendants) all-arguments))
               (inc sub-statements-count))
        {:sub-statements sub-statements-count}))))

(sub-discussion-information 101155069755552 96757023244400)

(comment
  (dialog-db/all-discussions-by-title "sadsad")
  (dialog-db/all-arguments-for-discussion 96757023244400)
  (let [db (d/db (db/new-connection))]
    (:e
      (first
        (d/datoms db {:index :vaet
                      :components [101155069755551 :argument/conclusion]}))))
  ;; TODO so könnte man sich funktionen definieren um die verschiedenen knoten
  ;; als datom zu holen. Dann könnte man diese normal durchqueren.
  )
