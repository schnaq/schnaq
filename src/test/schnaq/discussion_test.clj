(ns schnaq.discussion-test
  (:require [clojure.test :refer [use-fixtures is deftest testing are]]
            [schnaq.database.discussion :as db]
            [schnaq.discussion :as discussion]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest undercuts-for-root-test
  (testing "Test whether all undercut descendants are found in a sub-discussion."
    (let [undercuts-for-root @#'discussion/undercuts-for-root
          share-hash "ameisenbär-hash"
          all-arguments (db/all-arguments-for-discussion share-hash)
          matching-argument (first (filter #(= :argument.type/attack (:argument/type %)) all-arguments))
          root-statement (first (:argument/premises matching-argument))
          matching-undercut (first (undercuts-for-root (:db/id root-statement) all-arguments))]
      (is (= "going for a walk with the dog every day is good for social interaction and physical exercise"
             (-> matching-undercut :argument/premises first :statement/content)))
      (is (= "Der miese Peter" (-> matching-undercut :argument/author :user/nickname)))
      (is (empty? (undercuts-for-root [] all-arguments))))))

(deftest direct-children-test
  (testing "Test whether all direct children are found."
    (let [direct-children @#'discussion/direct-children
          share-hash "ameisenbär-hash"
          root-id (:db/id (first (db/starting-statements share-hash)))
          all-arguments (db/all-arguments-for-discussion share-hash)
          children (direct-children root-id all-arguments)]
      (is (= 2 (count children)))
      (is (empty? (direct-children -1 all-arguments))))))

(deftest sub-discussion-information-test
  (testing "Test information regarding sub-discussions."
    (let [share-hash "ameisenbär-hash"
          arguments (db/all-arguments-for-discussion share-hash)
          root-id (:db/id (first (db/starting-statements share-hash)))
          infos (discussion/sub-discussion-information root-id arguments)
          author-names (into #{} (map :user/nickname (:authors infos)))]
      (is (= 3 (:sub-statements infos)))
      (is (contains? author-names "Der miese Peter"))
      (is (contains? author-names "Wegi"))
      (is (contains? author-names "Der Schredder")))))

(deftest nodes-for-agenda-test
  (testing "Validate data for graph nodes."
    (let [share-hash "cat-dog-hash"
          statements (db/all-statements-for-graph share-hash)
          contents (set (map :label statements))
          nodes (discussion/nodes-for-agenda statements share-hash)
          statement-nodes (remove #(= :agenda (:type %)) nodes)]
      (testing "Nodes contain the discussion topic as data thus containing one more element than the statements."
        (is (= (count statements) (dec (count nodes)))))
      (testing "Only one agenda point."
        (is (= 1 (count (filter #(= :agenda (:type %)) nodes)))))
      (testing (str "Check if all content from statements is present in nodes.")
        (is (= (count statement-nodes) (count (filter #(contents (:label %)) statement-nodes))))))))

(deftest links-for-agenda-test
  (testing "Validate data for graph links"
    (let [share-hash "graph-hash"
          statements (db/all-statements-for-graph share-hash)
          starting-statements (db/starting-statements share-hash)
          links (discussion/links-for-starting starting-statements share-hash)]
      (testing "The number of nodes (statements + 1 topic) is always one higher than the links."
        (is (= (count statements) (count links)))))))

(deftest update-controversy-map-test
  (testing "Update of a controversy-map"
    (let [edge-1 {:to 123 :type :argument.type/support}
          edge-2 {:to 1234 :type :argument.type/attack}
          edge-3 {:to 1235 :type :argument.type/starting}]
      (are [x y]
        (= x (@#'discussion/update-controversy-map {} y))
        {123 {:positive 1}} edge-1
        {1234 {:negative 1}} edge-2
        {} edge-3)
      (is (= {123 {:positive 2}} (@#'discussion/update-controversy-map {123 {:positive 1}}
                                   {:to 123 :type :argument.type/support}))))))

(deftest single-controversy-val-test
  (testing "Calculate a single controversy-value"
    (let [con-vals-1 {:positive 1}
          con-vals-2 {:negative 1}
          con-vals-3 {:positive 1
                      :negative 1}]
      (are [x y]
        (= x (@#'discussion/single-controversy-val y))
        0 con-vals-1
        100.0 con-vals-2
        50.0 con-vals-3))))

(deftest calculate-controversy-test
  (testing "Test the creation of a controversy-map"
    (let [edges [{:to 123 :type :argument.type/support} {:to 1234 :type :argument.type/support}
                 {:to 1234 :type :argument.type/attack} {:to 1235 :type :argument.type/starting}]]
      (is (= {123 0
              1234 50.0}
             (discussion/calculate-controversy edges))))))

(deftest premises-undercutting-argument-with-conclusion-id-test
  (testing "Get annotated premises, that are undercutting an argument with a certain premise"
    (let [share-hash "simple-hash"
          starting-conclusion (first (db/starting-statements share-hash))
          premise-to-undercut-id (:db/id (first (db/children-for-statement (:db/id starting-conclusion))))
          desired-statement (first (discussion/premises-undercutting-argument-with-premise-id premise-to-undercut-id))]
      (is (= "Brainstorm hat nichts mit aktiv denken zu tun" (:statement/content desired-statement)))
      (is (= :argument.type/undercut (:meta/argument-type desired-statement))))))
