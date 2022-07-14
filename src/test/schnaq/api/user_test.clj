(ns schnaq.api.user-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :refer [kangaroo schnaqqi]]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private schnaqqi-keycloak-id
  (:user.registered/keycloak-id schnaqqi))

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

;; -----------------------------------------------------------------------------

(defn- role-management-request [user-token keycloak-id role verb]
  (-> {:request-method verb
       :uri (:path (api/route-by-name :api.admin.user/role))
       :body-params {:keycloak-id keycloak-id
                     :role role}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      m/decode-response-body))

(deftest add-role-test
  (testing "Adding a role to a user via API is okay for admin users."
    (role-management-request toolbelt/token-n2o-admin kangaroo-keycloak-id :role/admin :put)
    (is (contains?
         (:user.registered/roles (user-db/private-user-by-keycloak-id kangaroo-keycloak-id))
         :role/admin))))

(deftest remove-role-test
  (testing "Removing a role from a user via API is okay for admin users."
    (role-management-request toolbelt/token-n2o-admin kangaroo-keycloak-id :role/admin :delete)
    (is (empty?
         (:user.registered/roles (user-db/private-user-by-keycloak-id kangaroo-keycloak-id))))))
