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
