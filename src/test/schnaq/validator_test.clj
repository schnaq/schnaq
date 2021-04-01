(ns schnaq.validator-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.meeting.database :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.validator :refer [user-schnaq-admin?]]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest user-schnaq-admin?-test
  (testing "Test whether the admin is checked correctly."
    (is (not (user-schnaq-admin? "cat-dog-hash" "59456d4a-6950-47e8-88d8-a1a6a8de9276")))
    (db/transact [[:db/add [:discussion/share-hash "cat-dog-hash"] :discussion/admins
                   [:user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"]]])
    (is (user-schnaq-admin? "cat-dog-hash" "59456d4a-6950-47e8-88d8-a1a6a8de9276"))))
