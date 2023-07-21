(ns schnaq.database.question-box-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.question-box :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest create-qa-box!-test
  (testing "Check whether the poll object is created correctly."
    (let [share-hash "cat-dog-hash"
          new-qa-box (db/create-qa-box! share-hash true "Questions about Testing")
          unlabeled-qa-box (db/create-qa-box! share-hash true)
          results (:discussion/qa-boxes (fast-pull [:discussion/share-hash share-hash]))]
      (is (= 2 (count results)))
      (is (zero? (count (:qa-box/questions new-qa-box))))
      (is (nil? (:qa-box/label unlabeled-qa-box))))))
