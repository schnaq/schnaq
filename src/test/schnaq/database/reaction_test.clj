(ns schnaq.database.reaction-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.reaction :as db]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [share-hash "cat-dog-hash"
          some-statement-ids (map :db/id (discussion-db/all-statements share-hash))
          statement-1-id (first some-statement-ids)
          statement-2-id (second some-statement-ids)
          author-1-id (user-db/add-user-if-not-exists "Test-1")
          author-2-id (user-db/add-user-if-not-exists "Test-2")]
      (db/upvote-statement! statement-1-id author-1-id)
      (db/downvote-statement! statement-2-id author-1-id)
      (db/upvote-statement! statement-1-id author-2-id)
      (is (db/did-user-upvote-statement statement-1-id author-1-id))
      (is (db/did-user-downvote-statement statement-2-id author-1-id))
      (is (= 2 (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (= 1 (-> statement-2-id fast-pull :statement/downvotes count)))
      (is (zero? (-> statement-1-id fast-pull :statement/downvotes count)))
      ;; No up- and downvote for the same statement by the same user!
      (db/downvote-statement! (first some-statement-ids) author-1-id)
      (is (= 1 (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (= 1 (-> statement-1-id fast-pull :statement/downvotes count)))
      ;; Remove the up and downvotes now
      (db/remove-downvote! (first some-statement-ids) author-1-id)
      (db/remove-upvote! (first some-statement-ids) author-2-id)
      (is (zero? (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (zero? (-> statement-1-id fast-pull :statement/downvotes count))))))
