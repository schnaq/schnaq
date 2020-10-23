(ns schnaq.meeting.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.meeting.database :as db]
            [schnaq.meeting.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [cat-or-dog (:db/id (first (db/all-discussions-by-title "Cat or Dog?")))
          enriched-data (processors/with-votes (db/starting-arguments-by-discussion cat-or-dog))
          statements-only (filter #(contains? % :statement/version) enriched-data)
          upvotes-only (filter number? (map :meta/upvotes statements-only))
          downvotes-only (filter number? (map :meta/downvotes statements-only))]
      (is (= (count statements-only) (count upvotes-only) (count downvotes-only))))))

(deftest with-sub-discussion-information-test
  (testing "Testing enrichment with sub-discussion-information."
    (let [discussion-id (:db/id (first (db/all-discussions-by-title "Tapir oder Ameisenbär?")))
          arguments (db/all-arguments-for-discussion discussion-id)
          root-id (:db/id (:argument/conclusion (first (db/starting-arguments-by-discussion discussion-id))))
          processed-structure (processors/with-sub-discussion-information {:statement/content "foo"
                                                                           :db/id root-id} arguments)
          infos (:meta/sub-discussion-info processed-structure)
          author-names (into #{} (map :author/nickname (:authors infos)))]
      (is (= 3 (:sub-statements infos)))
      (is (contains? author-names "Der miese Peter"))
      (is (contains? author-names "Wegi"))
      (is (contains? author-names "Der Schredder"))
      (is (= "foo" (:statement/content processed-structure))))))
