(ns schnaq.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.processors :as processors]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [share-hash "cat-dog-hash"
          enriched-data (processors/with-aggregated-votes (discussion-db/all-statements share-hash) 123)
          upvotes-only (remove nil? (map :statement/upvotes enriched-data))
          downvotes-only (remove nil? (map :statement/downvotes enriched-data))]
      (is (= 18 (count enriched-data)))
      ;; When there are cumulative votes, they should also yield a result
      (is (= 3 (count upvotes-only) (count downvotes-only))))))

(deftest add-meta-info-test
  (testing "Test if meta info was correctly added to schnaq"
    (let [share-hash "ameisenbär-hash"
          discussion (discussion-db/discussion-by-share-hash share-hash)
          author (:discussion/author discussion)
          discussion-with-meta-info (processors/schnaq-default discussion)
          meta-info (#'processors/discussion-meta-info share-hash author)
          processed-meta-info (get discussion-with-meta-info :meta-info)]
      (is (= meta-info processed-meta-info)))))

(deftest meta-infos-test
  (testing "Tests if number of posts are correct and authors increase after adding a new one to the discussion\n"
    (let [share-hash "simple-hash"
          author {:user/nickname "Wegi"}
          all-statements (discussion-db/all-statements share-hash)
          total-count (count all-statements)
          meta-infos (#'processors/discussion-meta-info share-hash author)
          ;; add starting statement
          statement "Clojure can melt steelbeams"
          user-id (user-db/add-user "New Person")
          _ (discussion-db/add-starting-statement! share-hash user-id statement)
          ;; new meta infos
          new-meta-infos (#'processors/discussion-meta-info share-hash author)]
      (testing "Test if total count is correct"
        (is (= total-count (:all-statements meta-infos))))
      (testing "Test if total count is increased after adding a new statement"
        (is (= (inc total-count) (:all-statements new-meta-infos))))
      (testing "Test if author count is increased after adding a new statement by a new user"
        (is (= (inc (count (:authors meta-infos)))
               (count (:authors new-meta-infos))))))))
