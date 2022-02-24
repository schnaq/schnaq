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
          some-statements (discussion-db/all-statements share-hash)
          statement-1 (first some-statements)
          statement-2 (second some-statements)
          statement-1-upvotes (count (:statement/upvotes statement-1))
          statement-1-downvotes (count (:statement/downvotes statement-1))
          statement-2-downvotes (count (:statement/downvotes statement-2))
          statement-1-id (:db/id statement-1)
          statement-2-id (:db/id statement-2)
          author-1-id (user-db/add-user-if-not-exists "Test-1")
          author-2-id (user-db/add-user-if-not-exists "Test-2")]
      (db/upvote-statement! statement-1-id author-1-id)
      (db/downvote-statement! statement-2-id author-1-id)
      (db/upvote-statement! statement-1-id author-2-id)
      (is (db/did-user-upvote-statement statement-1-id author-1-id))
      (is (db/did-user-downvote-statement statement-2-id author-1-id))
      (is (= (+ 2 statement-1-upvotes) (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (= (inc statement-2-downvotes) (-> statement-2-id fast-pull :statement/downvotes count)))
      (is (= statement-1-downvotes (-> statement-1-id fast-pull :statement/downvotes count)))
      ;; No up- and downvote for the same statement by the same user!
      (db/downvote-statement! statement-1-id author-1-id)
      (is (= (inc statement-1-upvotes) (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (= (inc statement-1-downvotes) (-> statement-1-id fast-pull :statement/downvotes count)))
      ;; Remove the up and downvotes now
      (db/remove-downvote! statement-1-id author-1-id)
      (db/remove-upvote! statement-1-id author-2-id)
      (is (= statement-1-upvotes (-> statement-1-id fast-pull :statement/upvotes count)))
      (is (= statement-1-downvotes (-> statement-1-id fast-pull :statement/downvotes count))))))
