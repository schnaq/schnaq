(ns schnaq.auth.lib-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.auth.lib :as auth-lib]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

(deftest has-role?-test
  (testing "Valid roles should be true."
    (let [user-identity (schnaq-toolbelt/create-parsed-jwt "kangaroo" true)]
      (is (not (auth-lib/has-role? user-identity #{"razupaltuff"})))
      (is (auth-lib/has-role? user-identity shared-config/admin-roles)))))

(deftest beta-tester?-test
  (testing "Beta-tester fn."
    (testing "User has beta-tester role, so user shall pass."
      (is (auth-lib/beta-tester?
           {:identity {:realm_access {:roles (vec shared-config/beta-tester-roles)}}})))
    (testing "Admins also have beta access."
      (is (auth-lib/beta-tester?
           {:identity {:realm_access {:roles (vec shared-config/admin-roles)}}})))
    (testing "No roles, no access."
      (is (not (auth-lib/beta-tester? {:identity {:realm_access {:roles []}}})))
      (is (not (auth-lib/beta-tester? {:identity {}}))))))

(deftest pro-user?-test
  (testing "Verify that"
    (testing "user has beta-tester role, so user shall pass."
      (is (auth-lib/pro-user?
           {:sub kangaroo-keycloak-id
            :realm_access {:roles (vec shared-config/beta-tester-roles)}})))
    (testing "admins also have beta access."
      (is (auth-lib/pro-user?
           {:sub kangaroo-keycloak-id
            :realm_access {:roles (vec shared-config/admin-roles)}})))
    (testing "user with no roles have no access."
      (is (not (auth-lib/pro-user? {:sub kangaroo-keycloak-id
                                    :realm_access {:roles []}})))
      (is (not (auth-lib/pro-user? {:sub kangaroo-keycloak-id}))))
    (testing "pro-users have access."
      (let [pro-kangaroo (user-db/subscribe-pro-tier kangaroo-keycloak-id "sub_kangaroo" "cus_kangaroo")]
        (is (auth-lib/pro-user? {:sub (:user.registered/keycloak-id pro-kangaroo)}))))))
