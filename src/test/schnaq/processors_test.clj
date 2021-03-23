(ns schnaq.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.meta-info :as meta-info]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [share-hash "cat-dog-hash"
          enriched-data (processors/with-votes (discussion-db/all-arguments-for-discussion share-hash))
          wrongly-asserted-meta (filter :meta/upvotes enriched-data)
          statements-only (flatten (map :argument/premises enriched-data))
          upvotes-only (filter number? (map :meta/upvotes statements-only))
          downvotes-only (filter number? (map :meta/downvotes statements-only))]
      (is (= (count statements-only) (count upvotes-only) (count downvotes-only)))
      (is (zero? (count wrongly-asserted-meta))))))

(deftest add-meta-info-test
  (testing "Test if meta info was correctly added to schnaq"
    (let [share-hash "ameisenbär-hash"
          discussion (discussion-db/discussion-by-share-hash share-hash)
          author (:discussion/author discussion)
          discussion-with-meta-info (processors/add-meta-info-to-schnaq discussion)
          meta-info (meta-info/discussion-meta-info share-hash author)
          processed-meta-info (get discussion-with-meta-info :meta-info)]
      (is (= meta-info processed-meta-info)))))
