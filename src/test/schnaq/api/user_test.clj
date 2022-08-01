(ns schnaq.api.user-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def ^:private kangaroo-keycloak-id
  (:user.registered/keycloak-id kangaroo))

;; -----------------------------------------------------------------------------

(defn- get-admin-user-request [user-token keycloak-id]
  (-> {:request-method :get
       :uri (:path (api/route-by-name :api.admin/user))
       :query-params {:keycloak-id keycloak-id}}
      (toolbelt/mock-authorization-header user-token)
      test-app))

(deftest get-admin-user-test
  (testing "Admins can retrieve a user's personal information."
    (is (-> (get-admin-user-request toolbelt/token-n2o-admin kangaroo-keycloak-id)
            m/decode-response-body
            :user :user.registered/visited-schnaqs
            seq))))

(deftest get-admin-user-invalid-permissions-test
  (testing "Non-admin users can't access private user information."
    (is (= 403 (:status (get-admin-user-request toolbelt/token-schnaqqifant-user kangaroo-keycloak-id))))))

;; -----------------------------------------------------------------------------

(defn- put-admin-user-request [user-token user]
  (-> {:request-method :put
       :uri (:path (api/route-by-name :api.admin/user))
       :body-params {:user user}}
      (toolbelt/mock-authorization-header user-token)
      toolbelt/add-csrf-header
      test-app))

(deftest update-user-test
  (testing "Admin users can alter fields of any other users."
    (is (= 200 (:status (put-admin-user-request
                         toolbelt/token-n2o-admin
                         {:user.registered/keycloak-id kangaroo-keycloak-id
                          :user.registered/email "f@kema.il"}))))))

(deftest update-user-missing-permissions-test
  (testing "Other users cannot alter fields of other users."
    (is (= 403 (:status (put-admin-user-request
                         toolbelt/token-schnaqqifant-user
                         {:user.registered/keycloak-id kangaroo-keycloak-id
                          :user.registered/email "f@kema.il"}))))))

;; -----------------------------------------------------------------------------

(defn- delete-admin-user-request [user-token keycloak-id attribute value]
  (-> {:request-method :delete
       :uri (:path (api/route-by-name :api.admin/user))
       :body-params {:keycloak-id keycloak-id
                     :attribute attribute
                     :value value}}
      (toolbelt/mock-authorization-header user-token)
      toolbelt/add-csrf-header
      test-app))

(deftest delete-user-attribute-test
  (testing "Admin users can delete fields of any other users."
    (is (= 200 (:status (delete-admin-user-request
                         toolbelt/token-n2o-admin
                         kangaroo-keycloak-id
                         :user.registered/email
                         nil))))))

(deftest delete-user-attribute-missing-permissions-test
  (testing "Other users cannot delete fields of other users."
    (is (= 403 (:status (delete-admin-user-request
                         toolbelt/token-schnaqqifant-user
                         kangaroo-keycloak-id
                         :user.registered/email
                         nil))))))
