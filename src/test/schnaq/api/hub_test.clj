(ns schnaq.api.hub-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ring.mock.request :as mock]
            [schnaq.api.hub :as hub-api]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest hub-by-keycloak-name-test
  (let [sut #'hub-api/hub-by-keycloak-name
        keycloak-name "schnaqqifantenparty"
        request #(-> (mock/request :get (format "/hub/%s" keycloak-name))
                     (assoc-in [:identity :groups] [%])
                     (assoc-in [:params :keycloak-name] keycloak-name))]
    (testing "Only request with valid group memberships should be allowed."
      (is (= 200 (:status (sut (request "schnaqqifantenparty")))))
      (is (= 403 (:status (sut (request "wrong-group"))))))))
