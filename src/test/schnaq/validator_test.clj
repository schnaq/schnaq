(ns schnaq.validator-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :as db]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.validator :refer [user-schnaq-admin? user-moderator?]]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest user-schnaq-admin?-test
  (testing "Test whether the admin is checked correctly."
    (is (not (user-schnaq-admin? "cat-dog-hash" "59456d4a-6950-47e8-88d8-a1a6a8de9276")))
    (db/transact [[:db/add [:discussion/share-hash "cat-dog-hash"] :discussion/moderators
                   [:user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"]]])
    (is (user-schnaq-admin? "cat-dog-hash" "59456d4a-6950-47e8-88d8-a1a6a8de9276"))))

(deftest user-moderator?-test
  (let [user-wegi (user-db/user-id nil "59456d4a-6950-47e8-88d8-a1a6a8de9276")
        user-kangaroo (user-db/user-id nil "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        user-invalid (user-db/user-id nil "11111111-1111-1111-1111-111111111111")]
    (testing "Test whether the moderator is checked correctly."
      (is (user-moderator? "cat-dog-hash" user-wegi))
      (is (not (user-moderator? "cat-dog-hash" user-invalid))))
    (testing "Authors are moderators also"
      (is (user-moderator? "cat-dog-hash" user-kangaroo)))))
