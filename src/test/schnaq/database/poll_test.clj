(ns schnaq.database.poll-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.poll :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest new-poll-test
  (testing "Check whether the poll object is created correctly."
    (let [discussion-id (:db/id (fast-pull [:discussion/share-hash "cat-dog-hash"] '[:db/id]))
          new-poll (db/new-poll! "Test Poll" :poll.type/multiple-choice
                                 ["Eis" "Sorbet" "Joghurt"] discussion-id)
          failed-poll (db/new-poll! "Failed" :poll.type/single-choice
                                    [] discussion-id)]
      (is (zero? (apply + (map :option/votes (:poll/options new-poll)))))
      (is (= 3 (count (:poll/options new-poll))))
      (is (= :poll.type/multiple-choice (:poll/type new-poll)))
      (is (= "Cat or Dog?" (get-in new-poll [:poll/discussion :discussion/title])))
      (is (nil? failed-poll)))))

(deftest polls-test
  (testing "Correctly retrieve all polls for cat-dog-discussion"
    (let [polls (db/polls "cat-dog-hash")
          single (first (filter #(= :poll.type/single-choice (:poll/type %)) polls))
          multiple (first (filter #(= :poll.type/multiple-choice (:poll/type %)) polls))]
      (is (= 2 (count polls)))
      (is (= 3 (count (:poll/options single)) (count (:poll/options multiple))))
      (is (= "Ganz allein" (:poll/title single)))
      (is (= "Ganz allein mit mehreren" (:poll/title multiple)))
      (is (= 4 (apply + (map :option/votes (:poll/options single)))))
      (is (= 3 (apply + (map :option/votes (:poll/options multiple))))))))

(deftest vote!-test
  (let [share-hash "simple-hash"
        poll (first (db/polls share-hash))
        poll-id (:db/id poll)
        option (first (filter #(zero? (:option/votes %)) (:poll/options poll)))]
    (testing "A vote always increments the number when the option and share-hash match."
      (db/vote! (:db/id option) poll-id share-hash)
      (is (= 1 (:option/votes (fast-pull (:db/id option) '[:option/votes]))))
      (db/vote! (:db/id option) poll-id share-hash)
      (is (= 2 (:option/votes (fast-pull (:db/id option) '[:option/votes])))))
    (testing "Providing a non-matching share-hash should do nothing"
      (is (nil? (db/vote! (:db/id option) poll-id "Non-matching share hash 123")))
      (is (= 2 (:option/votes (fast-pull (:db/id option) '[:option/votes])))))
    (testing "Providing a non-matching poll-id should do nothing as well"
      (is (nil? (db/vote! (:db/id option) (inc poll-id) share-hash)))
      (is (= 2 (:option/votes (fast-pull (:db/id option) '[:option/votes])))))))

(deftest vote-multiple!-test
  (let [share-hash "cat-dog-hash"
        poll (first (filter #(= :poll.type/multiple-choice (:poll/type %))
                            (db/polls share-hash)))
        poll-id (:db/id poll)
        options (:poll/options poll)
        option-0 (first (filter #(zero? (:option/votes %)) options))
        option-1 (first (filter #(= 1 (:option/votes %)) options))
        option-2 (first (filter #(= 2 (:option/votes %)) options))
        all-option-ids [(:db/id option-0) (:db/id option-1) (:db/id option-2)]]
    (testing "A vote always increments the number when the options and share-hash match."
      (db/vote-multiple! all-option-ids poll-id share-hash)
      (is (= 1 (:option/votes (fast-pull (:db/id option-0) '[:option/votes]))))
      (is (= 2 (:option/votes (fast-pull (:db/id option-1) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-2) '[:option/votes])))))
    (testing "Providing a non-matching share-hash should do nothing"
      (is (nil? (db/vote-multiple! all-option-ids poll-id "Non-matching share hash 123")))
      (is (= 1 (:option/votes (fast-pull (:db/id option-0) '[:option/votes])))))
    (testing "Providing a non-matching option-id should ignore the particular option"
      (let [txs (db/vote-multiple! [(:db/id option-0) (:db/id option-1) (+ 99 (:db/id option-2))]
                                   poll-id share-hash)]
        (is (= 2 (count txs))))
      (is (= 2 (:option/votes (fast-pull (:db/id option-0) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-1) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-2) '[:option/votes])))))))
