(ns schnaq.database.main-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db]
            [schnaq.database.survey :as survey-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest clean-db-vals-test
  (testing "Test whether nil values are properly cleaned from a map."
    (let [no-change-map {:foo :bar
                         :baz :bam}
          time-map {:bar (db/now)}]
      (is (= no-change-map (@#'db/clean-db-vals no-change-map)))
      (is (= 2 (count (@#'db/clean-db-vals (merge no-change-map {:unwished-for nil})))))
      (is (= {} (@#'db/clean-db-vals {})))
      (is (= {} (@#'db/clean-db-vals {:foo ""})))
      (is (= time-map (@#'db/clean-db-vals time-map))))))

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
  (testing "Is the newly created object delivered corectly?"
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
  (let [survey (first (survey-db/surveys "simple-hash"))
        ;; Pattern adds default value of 0 where there is none
        option-with-vote-attr (first (filter #(not= 0 (:option/votes %)) (:survey/options survey))) ;; votes = 1
        option-without-vote-attr (first (filter #(zero? (:option/votes %)) (:survey/options survey)))]
    (testing "Show whether incrementing a number that's there works"
      (db/increment-number (:db/id option-with-vote-attr) :option/votes)
      (is (= 2 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes]))))
      (db/increment-number (:db/id option-with-vote-attr) :option/votes)
      (is (= 3 (:option/votes (db/fast-pull (:db/id option-with-vote-attr) '[:option/votes])))))
    (testing "Incrementing a value that is not there adds an attribute with the value 1"
      (db/increment-number (:db/id option-without-vote-attr) :option/votes)
      (is (= 1 (:option/votes (db/fast-pull (:db/id option-without-vote-attr) '[:option/votes])))))))
