(ns schnaq.api.hub-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ring.mock.request :as mock]
            [schnaq.api :as api]
            [schnaq.database.hub-test-data :as hub-test-data]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each
              toolbelt/init-test-delete-db-fixture
              #(toolbelt/init-test-delete-db-fixture % hub-test-data/hub-test-data))
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest hub-by-keycloak-name-test
  (let [keycloak-name "test-keycloak"
        request #(-> (mock/request :get (format "/hub/%s" keycloak-name))
                     (assoc-in [:identity :groups] [%]))]
    (testing "Only request with valid group memberships should be allowed."
      (is (= 200 (-> "test-keycloak" request api/app :status)))
      (is (= 403 (-> "some-other-invalid-group" request api/app :status))))))
