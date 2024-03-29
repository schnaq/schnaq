(ns schnaq.database.analytics-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.analytics :as db]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.toolbelt :as toolbelt])
  (:import (java.time Instant)))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private any-meeting-share-hash "aklsuzd98-234da-123d")

(defn- any-discussion
  []
  (discussion-db/new-discussion {:discussion/title "Bla"
                                 :discussion/share-hash any-meeting-share-hash
                                 :discussion/author (user-db/add-user-if-not-exists "Wegi")}))

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
    (is (= 28 (:overall (db/number-of-statements))))
    (is (zero? (:overall (db/number-of-statements (Instant/now)))))
    (let [user-id (user-db/add-user-if-not-exists "Wegi")
          share-hash "asd"]
      (discussion-db/new-discussion {:discussion/title "test"
                                     :discussion/share-hash share-hash
                                     :discussion/author user-id})
      (discussion-db/add-starting-statement! share-hash user-id "test")
      (is (= 29 (:overall (db/number-of-statements))))))
  (testing "The statements in the series should be grouped accordingly"
    (is (= 27 (get (:series (db/number-of-statements)) "2020-W01")))
    (is (= 1 (get (:series (db/number-of-statements)) "2020-W02")))))

(deftest average-number-of-statements-test
  (testing "Test whether the average number of statements fits."
    (is (= 28/4 (db/average-number-of-statements)))
    (any-discussion)
    (is (= 28/5 (db/average-number-of-statements)))))

(deftest number-of-active-users-test
  (testing "Test whether the active users are returned correctly."
    (let [initial-overall-active-users (:overall (db/number-of-active-discussion-users))
          woggler-id (user-db/add-user-if-not-exists "wooooggler")]
      (discussion-db/add-starting-statement! "cat-dog-hash" woggler-id "Alles doof")
      (is (= 4 initial-overall-active-users))
      (is (= 5 (:overall (db/number-of-active-discussion-users))))
      (is (= 1 (:overall/registered (db/number-of-active-discussion-users)))))))

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

(deftest users-created-since-test
  (testing "Count registered users."
    (is (zero? (count (db/users-created-since (Instant/now)))))
    (is (= 4 (count (db/users-created-since (toolbelt/now-minus-days 7)))))))

;; -----------------------------------------------------------------------------

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

(deftest number-of-pro-user-zero-test
  (testing "Number of pro users is zero in the beginning."
    (is (zero? (db/number-of-pro-users)))))

(deftest number-of-pro-user-one-pro-user-test
  (testing "Adding a pro user returns one pro user."
    (user-db/subscribe-pro-tier kangaroo-keycloak-id "sub_subscription-id" "cus_kangaroo" false)
    (is (= 1 (db/number-of-pro-users)))))

(deftest number-of-pro-user-unsubscribe-test
  (testing "Add a pro user and remove it afterwards, results in zero pro users."
    (user-db/subscribe-pro-tier kangaroo-keycloak-id "sub_subscription-id" "cus_kangaroo" false)
    (user-db/unsubscribe-pro-tier kangaroo-keycloak-id false)
    (is (zero? (db/number-of-pro-users)))))

(deftest statistics-for-users-by-email-patterns-test
  (testing "Query users by email patterns and count their created discussions."
    (is (zero? (get-in (db/statistics-for-users-by-email-patterns [#".*@schnaq\.com"]) [:discussions :total])))
    (is (<= 2 (get-in (db/statistics-for-users-by-email-patterns [#".*@schneider\.gg"]) [:discussions :total])))))
