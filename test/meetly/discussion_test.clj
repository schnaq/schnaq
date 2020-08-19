(ns meetly.discussion-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [dialog.discussion.database :as ddb]
            [meetly.discussion :as discussion]
            [meetly.test.toolbelt :as meetly-toolbelt]))

(use-fixtures :each meetly-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once meetly-toolbelt/clean-database-fixture)

(deftest descendant-undercuts-test
  (testing "Test whether all undercut descendants are found in a sub-discussion."
    (let [descendant-undercuts @#'discussion/descendant-undercuts
          discussion-id (:db/id (first (ddb/all-discussions-by-title "Tapir oder Ameisenbär?")))
          root-statement (ddb/starting-arguments-by-discussion discussion-id)
          all-arguments (ddb/all-arguments-for-discussion discussion-id)
          matching-undercut (first (descendant-undercuts root-statement all-arguments))]
      (is (= "going for a walk with the dog every day is good for social interaction and physical exercise"
             (-> matching-undercut :argument/premises first :statement/content)))
      (is (= "Der miese Peter" (-> matching-undercut :argument/author :author/nickname)))
      (is (empty? (descendant-undercuts [] all-arguments))))))

(deftest direct-children-test
  (testing "Test whether all direct children are found."
    (let [direct-children @#'discussion/direct-children
          discussion-id (:db/id (first (ddb/all-discussions-by-title "Tapir oder Ameisenbär?")))
          root-id (:db/id (:argument/conclusion (first (ddb/starting-arguments-by-discussion discussion-id))))
          all-arguments (ddb/all-arguments-for-discussion discussion-id)
          children (direct-children root-id all-arguments)]
      (is (= 3 (count children)))
      (is (empty? (direct-children -1 all-arguments))))))

(deftest sub-discussion-information-test
  (testing "Test information regarding sub-discussions."
    (let [discussion-id (:db/id (first (ddb/all-discussions-by-title "Tapir oder Ameisenbär?")))
          root-id (:db/id (:argument/conclusion (first (ddb/starting-arguments-by-discussion discussion-id))))
          infos (discussion/sub-discussion-information root-id discussion-id)
          author-names (into #{} (map :author/nickname (:authors infos)))]
      (is (= 3 (:sub-statements infos)))
      (is (contains? author-names "Der miese Peter"))
      (is (contains? author-names "Wegi"))
      (is (contains? author-names "Der Schredder")))))