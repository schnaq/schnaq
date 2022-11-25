(ns schnaq.validator-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.validator :refer [user-moderator? posts-allowed?]]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest user-moderator?-test
  (let [user-id-wegi (user-db/user-id nil "59456d4a-6950-47e8-88d8-a1a6a8de9276")
        user-id-kangaroo (user-db/user-id nil "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")
        user-id-invalid (user-db/user-id nil "11111111-1111-1111-1111-111111111111")]
    (testing "Test whether the moderator is checked correctly."
      (is (user-moderator? "cat-dog-hash" user-id-wegi))
      (is (not (user-moderator? "cat-dog-hash" user-id-invalid))))
    (testing "Authors are moderators also"
      (is (user-moderator? "cat-dog-hash" user-id-kangaroo)))))

(deftest posts-allowed?-test
  (testing "Write locked discussions need to be recognized."
    (is (not (posts-allowed? "public-share-hash-locked")))
    (is (posts-allowed? "cat-dog-hash"))
    (is (not (posts-allowed? "nonsense-hash-a98sdauishd")))))
