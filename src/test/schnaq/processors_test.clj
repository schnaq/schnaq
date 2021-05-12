(ns schnaq.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meta-info :as meta-info]
            [schnaq.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [share-hash "cat-dog-hash"
          enriched-data (processors/with-votes (discussion-db/all-statements share-hash))
          upvotes-only (filter number? (map :meta/upvotes enriched-data))
          downvotes-only (filter number? (map :meta/downvotes enriched-data))]
      (is (= (count enriched-data) (count upvotes-only) (count downvotes-only))))))

(deftest add-meta-info-test
  (testing "Test if meta info was correctly added to schnaq"
    (let [share-hash "ameisenbär-hash"
          discussion (discussion-db/discussion-by-share-hash share-hash)
          author (:discussion/author discussion)
          discussion-with-meta-info (processors/add-meta-info-to-schnaq discussion)
          meta-info (meta-info/discussion-meta-info share-hash author)
          processed-meta-info (get discussion-with-meta-info :meta-info)]
      (is (= meta-info processed-meta-info)))))
