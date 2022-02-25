(ns schnaq.database.reaction-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.reaction :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [share-hash "cat-dog-hash"
          some-statements (discussion-db/all-statements share-hash)
          statement-1 (first (remove :statement/downvotes
                                     (remove :statement/upvotes some-statements)))
          statement-2 (second (remove :statement/downvotes some-statements))
          statement-1-upvotes (count (:statement/upvotes statement-1))
          statement-1-downvotes (count (:statement/downvotes statement-1))
          statement-2-downvotes (count (:statement/downvotes statement-2))
          statement-1-id (:db/id statement-1)
          statement-2-id (:db/id statement-2)
          alex-user-id (:db/id (fast-pull [:user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"]))
          kangaroo-user-id (:db/id (fast-pull [:user.registered/keycloak-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"]))]
      (testing "Non-cummulative votes should be triggered by registered users."
        (db/upvote-statement! statement-1-id alex-user-id)
        (is (db/did-user-upvote-statement statement-1-id alex-user-id))
        (db/upvote-statement! statement-1-id kangaroo-user-id)
        (db/downvote-statement! statement-2-id kangaroo-user-id)
        (is (db/did-user-downvote-statement statement-2-id kangaroo-user-id))
        (is (= (+ 2 statement-1-upvotes) (-> statement-1-id fast-pull :statement/upvotes count)))
        (is (= (inc statement-2-downvotes) (-> statement-2-id fast-pull :statement/downvotes count)))
        (is (= statement-1-downvotes (-> statement-1-id fast-pull :statement/downvotes count))))
      (testing "No up- and downvote by the same registered user for the same statement"
        (db/downvote-statement! statement-1-id alex-user-id)
        (is (= (inc statement-1-upvotes) (-> statement-1-id fast-pull :statement/upvotes count)))
        (is (= (inc statement-1-downvotes) (-> statement-1-id fast-pull :statement/downvotes count))))
      (testing "Remove up and downvotes for registered users"
        (db/remove-downvote! statement-1-id alex-user-id)
        (db/remove-upvote! statement-1-id kangaroo-user-id)
        (is (= statement-1-upvotes (-> statement-1-id fast-pull :statement/upvotes count)))
        (is (= statement-1-downvotes (-> statement-1-id fast-pull :statement/downvotes count)))))))

(deftest cummulative-votes-tests
  (testing "Test cummulative vote manipulation"
    (let [share-hash "cat-dog-hash"
          some-statements (discussion-db/all-statements share-hash)
          statement-1 (first some-statements)
          statement-1-id (:db/id statement-1)
          statement-1-upvotes (:statement/cummulative-upvotes statement-1 0)
          statement-1-downvotes (:statement/cummulative-downvotes statement-1 0)]
      (db/upvote-anonymous-statement! statement-1-id)
      (is (= (inc statement-1-upvotes) (-> statement-1-id fast-pull :statement/cummulative-upvotes)))
      (db/downvote-anonymous-statement! statement-1-id)
      (is (= (inc statement-1-downvotes) (-> statement-1-id fast-pull :statement/cummulative-downvotes)))
      (db/remove-anonymous-upvote! statement-1-id)
      (is (= statement-1-upvotes (-> statement-1-id fast-pull :statement/cummulative-upvotes)))
      (db/remove-anonymous-downvote! statement-1-id)
      (is (= statement-1-upvotes (-> statement-1-id fast-pull :statement/cummulative-downvotes))))))
