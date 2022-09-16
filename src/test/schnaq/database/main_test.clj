(ns schnaq.database.main-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db :refer [discussion-by-share-hash]]
            [schnaq.database.main :as db :refer [set-activation-focus]]
            [schnaq.database.poll :as poll-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest add-feedback-test
  (testing "Valid feedbacks should be stored."
    (let [feedback {:feedback/description "Very good stuff 👍 Would use again"
                    :feedback/contact-mail "christian@schnaq.com"
                    :feedback/has-image? false}]
      (is (zero? (count (db/all-feedbacks))))
      (is (number? (db/add-feedback! feedback)))
      (is (= 1 (count (db/all-feedbacks)))))))

(deftest all-discussions-by-title-test
  (testing "Should return discussions if title matches at least one discussion."
    (is (empty? (discussion-db/all-discussions-by-title "")))
    (is (empty? (discussion-db/all-discussions-by-title "👾")))
    (is (seq (discussion-db/all-discussions-by-title "Cat or Dog?")))))

(deftest transact-and-pull-temp-test
  (testing "Is the newly created object delivered correctly?"
    (is (= "Hi thar" (:discussion/title
                      (db/transact-and-pull-temp
                       [{:db/id "test"
                         :discussion/title "Hi thar"
                         :discussion/share-hash "Bypassing all other fields"}]
                       "test" '[:discussion/title]))))
    (is (= :my-ident (:db/ident (db/transact-and-pull-temp
                                 [{:db/id "test2"
                                   :db/ident :my-ident}]
                                 "test2" '[*]))))))

(deftest increment-number-test
  (let [poll (first (poll-db/polls "simple-hash"))
        ;; Pattern adds default value of 0 where there is none
        option-with-vote-attr (first (filter #(not= 0 (:option/votes %)) (:poll/options poll))) ;; votes = 1
        option-without-vote-attr (first (filter #(zero? (:option/votes %)) (:poll/options poll)))]
    (testing "Show whether incrementing a number that's there works"
      (db/increment-number (:db/id option-with-vote-attr) :option/votes)
      (is (= 2 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes]))))
      (db/increment-number (:db/id option-with-vote-attr) :option/votes)
      (is (= 3 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes])))))
    (testing "Incrementing a value that is not there adds an attribute with the value 1"
      (db/increment-number (:db/id option-without-vote-attr) :option/votes)
      (is (= 1 (:option/votes (db/fast-pull (:db/id option-without-vote-attr) '[:option/votes])))))))

(deftest decrement-number-test
  (let [poll (first (poll-db/polls "simple-hash"))
        ;; Pattern adds default value of 0 where there is none
        option-with-vote-attr (first (filter #(not= 0 (:option/votes %)) (:poll/options poll))) ;; votes = 1
        option-without-vote-attr (first (filter #(zero? (:option/votes %)) (:poll/options poll)))]
    (testing "Show whether decrementing a number that's there works"
      (db/decrement-number (:db/id option-with-vote-attr) :option/votes)
      (is (= 0 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes]))))
      (db/decrement-number (:db/id option-with-vote-attr) :option/votes)
      (is (= -1 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes])))))
    (testing "Decrementing a value that is not there adds an attribute with the value -1"
      (db/decrement-number (:db/id option-without-vote-attr) :option/votes)
      (is (= -1 (:option/votes (db/fast-pull (:db/id option-without-vote-attr) '[:option/votes])))))
    (testing "Do not allow decreasing lower than the minimum value"
      (db/decrement-number (:db/id option-without-vote-attr) :option/votes 0)
      (is (zero? (:option/votes (db/fast-pull (:db/id option-without-vote-attr) '[:option/votes])))))))

(deftest set-activation-focus-empty-test
  (testing "If no activation is set as focus, no id is stored in discussion."
    (let [discussion (discussion-db/discussion-by-share-hash "cat-dog-hash")]
      (is (nil? (:discussion/activation-focus discussion))))))

(deftest set-activation-focus-test
  (testing "Set a poll as a focus and its id is stored in the discussion."
    (let [share-hash "cat-dog-hash"
          poll-id (:db/id (first (poll-db/polls share-hash)))
          _ (set-activation-focus [:discussion/share-hash share-hash] poll-id)]
      (is (= poll-id
             (-> share-hash discussion-by-share-hash :discussion/activation-focus))))))
