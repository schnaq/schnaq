(ns schnaq.database.user-deletion-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.user-deletion :as user-deletion]
            [schnaq.test-data :as test-data :refer [kangaroo alex]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

(def ^:private alex-keycloak-id
  (:user.registered/keycloak-id alex))

(deftest delete-all-statements-for-user-test
  (testing "Delete all statements for a given user.")
  (let [_ (user-deletion/delete-all-statements-for-user kangaroo-keycloak-id)]
    (is (zero? (count (discussion-db/all-statements-from-user kangaroo-keycloak-id))))))

(deftest delete-discussions-for-user-test
  (testing "Delete all discussions for a given user."
    (let [discussions #(map :discussion/share-hash (discussion-db/discussions-from-user alex-keycloak-id))]
      (is (= 1 (count (discussions))))
      (user-deletion/delete-discussions-for-user alex-keycloak-id)
      (is (zero? (count (discussions)))))))

(deftest delete-user-identity-test
  (let [kangaroo-db-before (fast-pull [:user.registered/keycloak-id kangaroo-keycloak-id])
        _ (user-deletion/delete-user-identity kangaroo-keycloak-id)
        kangaroo-db-after (fast-pull [:user.registered/keycloak-id kangaroo-keycloak-id])]
    (is (not= kangaroo-db-before kangaroo-db-after))))
