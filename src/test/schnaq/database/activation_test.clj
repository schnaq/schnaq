(ns schnaq.database.activation-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.activation :as activation-db]
            [schnaq.database.main :as db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest start-activation-test
  (testing "Test Activation creation and counter."
    (let [activation (activation-db/start-activation! "cat-dog-hash")]
      (is (not (nil? activation)))
      (is (zero? (:activation/count activation))))))

(deftest increase-activation-counter-test
  (let [share-hash "simple-hash"]
    (testing "Increase Counter."
      (let [activation-0 (activation-db/activation-by-share-hash share-hash)
            _inc-counter (activation-db/increment-activation! share-hash)
            activation-1 (activation-db/activation-by-share-hash share-hash)]
        (is (= (inc (:activation/count activation-0))
               (:activation/count activation-1)))))))

(deftest reset-activation-counter-test
  (testing "Reset activation counter."
    (let [share-hash "simple-hash"
          _ (activation-db/reset-activation! share-hash)
          activation (activation-db/activation-by-share-hash share-hash)]
      (is (zero? (:activation/count activation))))))

(deftest delete-activation!-test
  (testing "Delete activation by share-hash."
    (let [activation (activation-db/activation-by-share-hash "simple-hash")]
      (db/delete-entity! (:db/id activation))
      (is (nil? (activation-db/activation-by-share-hash "simple-hash"))))))
