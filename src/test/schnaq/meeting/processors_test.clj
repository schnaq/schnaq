(ns schnaq.meeting.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

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

(deftest with-sub-discussion-information-test
  (testing "Testing enrichment with sub-discussion-information."
    (let [share-hash "ameisenbär-hash"
          arguments (discussion-db/all-arguments-for-discussion share-hash)
          root-id (:db/id (first (discussion-db/starting-statements share-hash)))
          processed-structure (processors/with-sub-discussion-information {:statement/content "foo"
                                                                           :db/id root-id} arguments)
          infos (:meta/sub-discussion-info processed-structure)
          author-names (into #{} (map :author/nickname (:authors infos)))]
      (is (= 3 (:sub-statements infos)))
      (is (contains? author-names "Der miese Peter"))
      (is (contains? author-names "Wegi"))
      (is (contains? author-names "Der Schredder"))
      (is (= "foo" (:statement/content processed-structure))))))
