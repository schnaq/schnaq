(ns schnaq.auth.lib-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.auth.lib :as auth-lib]
            [schnaq.config.shared :as shared-config]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(deftest has-role?-test
  (testing "Valid roles should be true."
    (let [user-identity (schnaq-toolbelt/create-parsed-jwt "kangaroo" true)]
      (is (not (auth-lib/has-role? user-identity #{"razupaltuff"})))
      (is (auth-lib/has-role? user-identity shared-config/admin-roles)))))

(deftest beta-tester?-test
  (testing "User has beta-tester role, so user shall pass."
    (is (auth-lib/beta-tester?
         {:identity {:realm_access {:roles (vec shared-config/beta-tester-roles)}}})))
  (testing "Admins also have beta access."
    (is (auth-lib/beta-tester?
         {:identity {:realm_access {:roles (vec shared-config/admin-roles)}}})))
  (testing "No roles, no access."
    (is (not (auth-lib/beta-tester? {:identity {:realm_access {:roles []}}})))
    (is (not (auth-lib/beta-tester? {:identity {}})))))
