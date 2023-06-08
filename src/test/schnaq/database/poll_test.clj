(ns schnaq.database.poll-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.poll :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest new-poll-test
  (testing "Check whether the poll object is created correctly."
    (let [new-poll (db/new-poll! "cat-dog-hash" "Test Poll" :poll.type/multiple-choice
                                 ["Eis" "Sorbet" "Joghurt"]
                                 false)
          failed-poll (db/new-poll! "cat-dog-hash"
                                    "Failed" :poll.type/single-choice []
                                    false)]
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
      (is (= 3 (count polls)))
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
      (db/vote! share-hash poll-id (:db/id option))
      (is (= 1 (:option/votes (fast-pull (:db/id option) '[:option/votes]))))
      (db/vote! share-hash poll-id (:db/id option))
      (is (= 2 (:option/votes (fast-pull (:db/id option) '[:option/votes])))))
    (testing "Providing a non-matching share-hash should do nothing"
      (is (nil? (db/vote! "Non-matching share hash 123" poll-id (:db/id option))))
      (is (= 2 (:option/votes (fast-pull (:db/id option) '[:option/votes])))))
    (testing "Providing a non-matching poll-id should do nothing as well"
      (is (nil? (db/vote! share-hash (inc poll-id) (:db/id option))))
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
      (db/vote-multiple! share-hash poll-id all-option-ids)
      (is (= 1 (:option/votes (fast-pull (:db/id option-0) '[:option/votes]))))
      (is (= 2 (:option/votes (fast-pull (:db/id option-1) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-2) '[:option/votes])))))
    (testing "Providing a non-matching share-hash should do nothing"
      (is (nil? (db/vote-multiple! "Non-matching share hash 123" poll-id all-option-ids)))
      (is (= 1 (:option/votes (fast-pull (:db/id option-0) '[:option/votes])))))
    (testing "Providing a non-matching option-id should ignore the particular option"
      (let [txs (db/vote-multiple! share-hash poll-id
                                   [(:db/id option-0) (:db/id option-1) (+ 99 (:db/id option-2))])]
        (is (= 2 (count txs))))
      (is (= 2 (:option/votes (fast-pull (:db/id option-0) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-1) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-2) '[:option/votes])))))))

(deftest delete-poll!-test
  (testing "Deleting a poll from a discussion reduces the total amount of polls."
    (let [polls (db/polls "cat-dog-hash")
          poll-id (-> polls first :db/id)
          _ (db/delete-poll! "cat-dog-hash" poll-id)
          polls-after (db/polls "cat-dog-hash")]
      (is (= (dec (count polls)) (count polls-after))))))

(deftest delete-poll-wrong-hash!-test
  (testing "Do not delete poll if share-hash is invalid."
    (let [polls (db/polls "cat-dog-hash")
          poll-id (-> polls first :db/id)
          _ (db/delete-poll! "definitely-wrong" poll-id)
          polls-after (db/polls "cat-dog-hash")]
      (is (= (count polls) (count polls-after))))))

(deftest poll-from-discussion-test
  (let [polls (db/polls "cat-dog-hash")
        poll-id (:db/id (first polls))
        poll (db/poll-from-discussion "cat-dog-hash" poll-id)]
    (is (= poll-id (:db/id poll)))))

(deftest poll-from-discussion-false-test
  (let [polls (db/polls "cat-dog-hash")
        poll-id (:db/id (first polls))
        poll (db/poll-from-discussion "cat-dog-hash" (inc poll-id))]
    (is (nil? (:db/id poll)))))

(deftest vote-ranking!-test
  (testing "Check whether vote for ranking type polls work correctly."
    (let [share-hash "cat-dog-hash"
          poll (first (filter #(= :poll.type/ranking (:poll/type %)) (db/polls share-hash)))
          options (:poll/options poll)
          option-0 (first (filter #(zero? (:option/votes %)) options))
          option-1 (first (filter #(= 1 (:option/votes %)) options))
          option-2 (first (filter #(= 2 (:option/votes %)) options))
          all-option-ids [(:db/id option-0) (:db/id option-1) (:db/id option-2)]]
      (db/vote-ranking! share-hash (:db/id poll) all-option-ids)
      (is (= 3 (:option/votes (fast-pull (:db/id option-0) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-1) '[:option/votes]))))
      (is (= 3 (:option/votes (fast-pull (:db/id option-2) '[:option/votes])))))))

(deftest toggle-poll-hide-results-test
  (let [share-hash "cat-dog-hash"
        poll-id (-> share-hash db/polls first :db/id)
        _ (db/toggle-hide-poll-results share-hash poll-id true)
        poll (db/poll-from-discussion "cat-dog-hash" poll-id)]
    (testing "Flag to hide poll results can be set."
      (is (:poll/hide-results? poll)))))

(deftest edit-poll-options-test
  (testing "Check whether editing polls works as expected."
    (let [share-hash "simple-hash"
          poll (first (db/polls share-hash))
          with-vote (first (filter #(= 1 (:option/votes %)) (:poll/options poll)))
          without-vote (first (filter #(not= 1 (:option/votes %)) (:poll/options poll)))
          new-poll (db/edit-poll share-hash (:db/id poll) "polly" true
                                 ["new" "new"]
                                 [(:db/id with-vote)]
                                 [{:id (:db/id without-vote) :value "very new"}])]
      (is (= "polly" (:poll/title new-poll)))
      (is (:poll/hide-results? new-poll))
      (is (= 3 (count (:poll/options new-poll))))
      (is (= 2 (count (filter #(= "new" (:option/value %)) (:poll/options new-poll)))))
      (is (not (first (filter #(= 1 (:option/votes %)) (:poll/options new-poll)))))
      (is (= 1 (count (filter #(= "very new" (:option/value %)) (:poll/options new-poll)))))
      (is (= 0 (count (filter #(= "Ohne Vote" (:option/value %)) (:poll/options new-poll))))))))
