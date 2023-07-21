(ns schnaq.database.question-box-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.question-box :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest create-qa-box!-test
  (testing "Check whether the poll object is created correctly."
    (let [share-hash "cat-dog-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          unlabeled-qa-box (db/create-qa-box! share-hash true)
          results (:discussion/qa-boxes (fast-pull [:discussion/share-hash share-hash]))]
      (is (= 2 (count results)))
      (is (zero? (count (:qa-box/questions new-qa-box))))
      (is (nil? (:qa-box/label unlabeled-qa-box))))))

(deftest delete-qa-box!-test
  (testing "Deleting a question-box works as expected."
    (let [share-hash "simple-hash"
          qa-box-id (get-in (fast-pull [:discussion/share-hash share-hash] '[{:discussion/qa-boxes [:db/id]}])
                            [:discussion/qa-boxes 0 :db/id])
          _tx @(db/delete-qa-box! qa-box-id)
          qa-boxes (:discussion/qa-boxes (fast-pull [:discussion/share-hash share-hash]))]
      (is (zero? (count qa-boxes))))))

(deftest update-qa-box-test
  (testing "Properly change visibility and label of the qa-box."
    (let [share-hash "simple-hash"
          qa-box-id (-> (fast-pull [:discussion/share-hash share-hash] '[{:discussion/qa-boxes [:db/id]}])
                        :discussion/qa-boxes first :db/id)
          updated-qa-box @(db/update-qa-box qa-box-id false "New Label")]
      (is (false? (:qa-box/visible updated-qa-box)))
      (is (= "New Label" (:qa-box/label updated-qa-box))))))

(deftest add-question-test
  (testing "That a question is added to a qa-box."
    (let [share-hash "simple-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          added-question (db/add-question (:db/id new-qa-box) "What is love?")
          qa-box (fast-pull (:db/id new-qa-box) patterns/qa-box)]
      (is (zero? (:qa-box.question/upvotes added-question)))
      (is (= 1 (count (:qa-box/questions qa-box))))
      (is (= "What is love?" (-> qa-box :qa-box/questions first :qa-box.question/value))))))

(deftest upvote-question-test
  (testing "that an upvote is registered correctly"
    (let [share-hash "simple-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          added-question (db/add-question (:db/id new-qa-box) "What is love?")
          _ (db/upvote-question (:db/id added-question))
          _ (db/upvote-question (:db/id added-question))
          question (fast-pull (:db/id added-question) patterns/question)]
      (is (= 2 (:qa-box.question/upvotes question))))))

(deftest mark-question-test
  (testing "that a question is correctly marked as answered"
    (let [share-hash "simple-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          added-question (db/add-question (:db/id new-qa-box) "What is love?")
          answered-question (db/mark-question (:db/id added-question))]
      (is (:qa-box.question/answered answered-question)))))

(deftest delete-question-test
  (testing "That a question is deleted properly."
    (let [share-hash "simple-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          added-question (db/add-question (:db/id new-qa-box) "What is love?")
          _ (db/delete-question (:db/id added-question))
          qa-box (fast-pull (:db/id new-qa-box) patterns/qa-box)]
      (is (zero? (count (:qa-box/questions qa-box)))))))
