(ns schnaq.database.analytics-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.analytics :as db]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
            [schnaq.database.user :as user-db]
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
                                 :discussion/edit-hash (str any-meeting-share-hash "-secret")
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
                                     :discussion/edit-hash "ahsdasd"
                                     :discussion/share-hash share-hash
                                     :discussion/author user-id})
      (discussion-db/add-starting-statement! share-hash user-id "test" false)
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
    (is (= 4 (:overall (db/number-of-active-discussion-users))))
    (let [woggler-id (user-db/add-user-if-not-exists "wooooggler")]
      (is (= 4 (:overall (db/number-of-active-discussion-users))))
      (main-db/transact
       [(discussion-db/add-starting-statement! "cat-dog-hash" woggler-id "Alles doof" false)]))
    (is (= 5 (:overall (db/number-of-active-discussion-users))))
    (is (= 1 (:overall/registered (db/number-of-active-discussion-users))))))

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

(deftest number-of-pro-users-test
  (testing "The number of pro users can be determined by subscription type"
    (let [stripe-id "123132"]
      (is (zero? (db/number-of-pro-users)))
      (main-db/transact [{:user.registered.subscription/stripe-id stripe-id
                          :user.registered.subscription/stripe-customer-id "whatever"
                          :user.registered.subscription/type :user.registered.subscription.type/pro}])
      (is (= 1 (db/number-of-pro-users)))
      (let [sub-id (main-db/query '[:find ?subscription .
                                    :in $
                                    :where [?subscription :user.registered.subscription/stripe-id "123132"]])]
        (main-db/transact [[:db/retract sub-id :user.registered.subscription/type]]))
      (is (zero? (db/number-of-pro-users))))))
