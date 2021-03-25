(ns schnaq.database.reaction-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.reaction :as db]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [share-hash "cat-dog-hash"
          some-statements (map #(-> % :argument/premises first :db/id)
                               (discussion-db/all-arguments-for-discussion share-hash))
          author-1-id (user-db/add-user-if-not-exists "Test-1")
          author-2-id (user-db/add-user-if-not-exists "Test-2")]
      (db/upvote-statement! (first some-statements) author-1-id)
      (db/downvote-statement! (second some-statements) author-1-id)
      (db/upvote-statement! (first some-statements) author-2-id)
      (is (db/did-user-upvote-statement (first some-statements) author-1-id))
      (is (db/did-user-downvote-statement (second some-statements) author-1-id))
      (is (= 2 (db/upvotes-for-statement (first some-statements))))
      (is (= 1 (db/downvotes-for-statement (second some-statements))))
      (is (zero? (db/downvotes-for-statement (first some-statements))))
      ;; No up- and downvote for the same statement by the same user!
      (db/downvote-statement! (first some-statements) author-1-id)
      (is (= 1 (db/upvotes-for-statement (first some-statements))))
      (is (= 1 (db/downvotes-for-statement (first some-statements))))
      ;; Remove the up and downvotes now
      (db/remove-downvote! (first some-statements) author-1-id)
      (db/remove-upvote! (first some-statements) author-2-id)
      (is (zero? (db/upvotes-for-statement (first some-statements))))
      (is (zero? (db/downvotes-for-statement (first some-statements)))))))