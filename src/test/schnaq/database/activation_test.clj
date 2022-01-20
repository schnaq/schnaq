(ns schnaq.database.activation-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.activation :as activation-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest increase-activation-counter
  (let [share-hash "cat-dog-hash"]
    (testing "Test Activation creation and counter."
      (let [activation-0 (activation-db/start-activation! share-hash)]
        (is (not (nil? activation-0)))
        (is (zero? (:activation/count activation-0)))
        (testing "Increase Counter by 1."
          (let [_inc-counter (activation-db/increase-activation! share-hash)
                activation-1 (activation-db/activation-by-share-hash share-hash)]
            (is (= (inc (:activation/count activation-0))
                   (:activation/count activation-1)))))))))

(deftest increase-and-reset-activation-counter
  (let [share-hash "cat-dog-hash"]
    (testing "Test increase and reset."
      (let [activation-0 (activation-db/start-activation! share-hash)
            max-inc 30]
        (doseq [x (range 1 (inc max-inc))
                :let [_ (activation-db/increase-activation! share-hash)
                      activation-inc (activation-db/activation-by-share-hash share-hash)]]
          (testing (str "Increase counter by: " x)
            (is (= (+ x (:activation/count activation-0))
                   (:activation/count activation-inc)))))
        (testing "Reset counter."
          (let [_ (activation-db/reset-activation! share-hash)
                activation-reset (activation-db/activation-by-share-hash share-hash)]
            (is (zero? (:activation/count activation-reset)))))))))
