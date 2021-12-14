(ns schnaq.notification-service.core-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.main :as main-db]
            [schnaq.notification-service.core :as sut]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest discussions-with-new-statements-in-interval-test
  (let [discussions-with-new-statements-in-interval #'sut/discussions-with-new-statements-in-interval]
    (testing "There should be 18 new statements after initializing the new database."
      (is (= 18 (-> (discussions-with-new-statements-in-interval (main-db/days-ago 1) :notification-mail-interval/daily)
                    (get "cat-dog-hash")
                    :new-statements))))))
