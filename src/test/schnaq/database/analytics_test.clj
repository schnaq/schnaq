(ns schnaq.database.analytics-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.analytics :as db]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
            [schnaq.database.user :as user-db]
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
                                 :discussion/author (user-db/add-user-if-not-exists "Wegi")}
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
    (is (= 3 (db/number-of-usernames)))
    (user-db/add-user-if-not-exists "Some-Testdude")
    (is (= 4 (db/number-of-usernames)))
    (is (zero? (db/number-of-usernames (Instant/now))))))

(deftest number-of-statements-test
  (testing "Return the correct number of statements."
    (is (= 28 (db/number-of-statements)))
    (is (zero? (db/number-of-statements (Instant/now))))
    (let [user-id (user-db/add-user-if-not-exists "Wegi")
          share-hash "asd"]
      (discussion-db/new-discussion {:discussion/title "test"
                                     :discussion/edit-hash "ahsdasd"
                                     :discussion/share-hash share-hash
                                     :discussion/author user-id} true)
      (discussion-db/add-starting-statement! share-hash user-id "test" false)
      (is (= 29 (db/number-of-statements))))))

(deftest average-number-of-statements-test
  (testing "Test whether the average number of statements fits."
    (is (= 28/4 (db/average-number-of-statements)))
    (any-discussion)
    (is (= 28/5 (db/average-number-of-statements)))))

(deftest number-of-active-users-test
  (testing "Test whether the active users are returned correctly."
    (is (= 3 (db/number-of-active-discussion-users)))
    (let [woggler-id (user-db/add-user-if-not-exists "wooooggler")]
      (is (= 3 (db/number-of-active-discussion-users)))
      (main-db/transact
        [(discussion-db/add-starting-statement! "cat-dog-hash" woggler-id "Alles doof" false)]))
    (is (= 4 (db/number-of-active-discussion-users)))))

(deftest statement-length-stats-test
  (testing "Testing the function that returns lengths of statements statistics"
    (let [stats (db/statement-length-stats)]
      (is (< (:min stats) (:max stats)))
      (is (< (:min stats) (:median stats)))
      (is (> (:max stats) (:median stats)))
      (is (> (:max stats) (:average stats)))
      (is float? (:average stats)))))

(deftest statement-type-stats-test
  (testing "Statistics about statement types should be working."
    (let [stats (db/statement-type-stats)]
      (is (= 7 (:attacks stats)))
      (is (= 15 (:supports stats)))
      (is (= 0 (:neutrals stats))))))