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
          enriched-data (processors/with-aggregated-votes (discussion-db/all-statements share-hash) 123)
          upvotes-only (map :statement/upvotes enriched-data)
          downvotes-only (map :statement/downvotes enriched-data)]
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

(deftest remove-invalid-access-codes-test
  (let [discussion-with-access-code {:db/id 17592186045433,
                                     :discussion/author {:db/id 17592186045431, :user/nickname "penguin"},
                                     :discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0",
                                     :discussion/created-at #inst"2021-10-06T08:15:00.073-00:00",
                                     :discussion/title "Huhu was geht hier ab",
                                     :discussion/access {:db/id 17592186045510,
                                                         :discussion.access/code 43236077,
                                                         :discussion.access/created-at #inst"2021-10-06T12:34:22.363-00:00",
                                                         :discussion.access/expires-at #inst"2021-10-07T14:34:22.363-00:00"}}]
    (testing "Valid access-code stays in discussion-map."
      (is (:discussion/access (processors/remove-invalid-access-codes discussion-with-access-code))))
    (testing "Remove invalid access-codes from map."
      (let [discussion (assoc-in discussion-with-access-code [:discussion/access :discussion.access/expires-at] #inst"2000-10-06T12:34:22.363-00:00")]
        (is (nil? (:discussion/access (processors/remove-invalid-access-codes discussion))))))))
