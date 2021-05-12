(ns schnaq.discussion-test
  (:require [clojure.test :refer [use-fixtures is deftest testing are]]
            [schnaq.database.discussion :as db]
            [schnaq.discussion :as discussion]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

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
    (let [edge-1 {:to 123 :type :statement.type/support}
          edge-2 {:to 1234 :type :statement.type/attack}
          edge-3 {:to 1235 :type :statement.type/starting}]
      (are [x y]
        (= x (@#'discussion/update-controversy-map {} y))
        {123 {:positive 1}} edge-1
        {1234 {:negative 1}} edge-2
        {} edge-3)
      (is (= {123 {:positive 2}} (@#'discussion/update-controversy-map {123 {:positive 1}}
                                   {:to 123 :type :statement.type/support}))))))

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
    (let [edges [{:to 123 :type :argument.type/support} {:to 1234 :type :statement.type/support}
                 {:to 1234 :type :argument.type/attack} {:to 1235 :type :statement.type/starting}]]
      (is (= {123 0
              1234 50.0}
             (discussion/calculate-controversy edges))))))
