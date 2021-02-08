(ns schnaq.database.analytics-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.analytics :as db]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as main-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt])
  (:import (java.time Instant)))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private any-meeting-share-hash "aklsuzd98-234da-123d")

(defn- any-discussion
  []
  (discussion-db/new-discussion {:discussion/title "Bla"
                                 :discussion/share-hash any-meeting-share-hash
                                 :discussion/edit-hash (str any-meeting-share-hash "-secret")
                                 :discussion/author (main-db/add-user-if-not-exists "Wegi")}
                                true))

(deftest number-of-discussions-test
  (testing "Return the correct number of meetings"
    (is (= 4 (db/number-of-discussions)))
    ;; Adds any new discussion
    (any-discussion)
    (is (= 5 (db/number-of-discussions)))
    (is (zero? (db/number-of-discussions (Instant/now))))))

(deftest number-of-usernames-test
  (testing "Return the correct number of usernames"
    ;; There are at least the 4 users from the test-set
    (is (= 6 (db/number-of-usernames)))
    (main-db/add-user-if-not-exists "Some-Testdude")
    (is (= 7 (db/number-of-usernames)))
    (is (zero? (db/number-of-usernames (Instant/now))))))

(deftest number-of-statements-test
  (testing "Return the correct number of statements."
    (is (= 38 (db/number-of-statements)))
    (is (zero? (db/number-of-statements (Instant/now))))))

(deftest average-number-of-statements-test
  (testing "Test whether the average number of statements fits."
    (is (= 38/4 (db/average-number-of-statements)))
    (any-discussion)
    (is (= 38/5 (db/average-number-of-statements)))))

(deftest number-of-active-users-test
  (testing "Test whether the active users are returned correctly."
    (let [cat-or-dog-id (:db/id (first (discussion-db/all-discussions-by-title "Cat or Dog?")))]
      (is (= 4 (db/number-of-active-discussion-users)))
      (let [_ (main-db/add-user-if-not-exists "wooooggler")
            woggler-id (main-db/user-by-nickname "wooooggler")]
        (is (= 4 (db/number-of-active-discussion-users)))
        (main-db/transact
          [(discussion-db/prepare-new-argument cat-or-dog-id woggler-id "Alles doof"
                                               ["weil alles doof war"])]))
      (is (= 5 (db/number-of-active-discussion-users))))))

(deftest statement-length-stats-test
  (testing "Testing the function that returns lengths of statements statistics"
    (let [stats (main-db/statement-length-stats)]
      (is (< (:min stats) (:max stats)))
      (is (< (:min stats) (:median stats)))
      (is (> (:max stats) (:median stats)))
      (is (> (:max stats) (:average stats)))
      (is float? (:average stats)))))

(deftest argument-type-stats-test
  (testing "Statistics about argument types should be working."
    (let [stats (main-db/argument-type-stats)]
      (is (= 7 (:attacks stats)))
      (is (= 15 (:supports stats)))
      (is (= 9 (:undercuts stats))))))