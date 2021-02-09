(ns schnaq.api.analytics-test
  (:require [clojure.test :refer [use-fixtures deftest is are testing]]
            [ring.mock.request :as mock]
            [schnaq.api.analytics :as sut]
            [schnaq.test.toolbelt :as toolbelt :refer [token-schnaqqifant-user token-n2o-admin]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- response-status [path token]
  (:status (sut/analytics-routes
             (-> (mock/request :get (format "/analytics%s" path))
                 (toolbelt/mock-authorization-header token)))))

(defn- response-status-main-route [token]
  (:status
    (sut/analytics-routes
      (-> (mock/request :get "/analytics")
          (toolbelt/mock-query-params :days-since "7")
          (toolbelt/mock-authorization-header token)))))

(deftest permission-test
  (testing "Analytics should only be accessible from logged in super users."
    (are [path]
      (= 200 (response-status path token-n2o-admin))
      "/active-users"
      "/statements-per-discussion"
      "/active-users"
      "/discussions"
      "/statement-lengths"
      "/statements"
      "/usernames"))
  (testing "Wrongly logged in users have no permission."
    (are [path]
      (= 403 (response-status path token-schnaqqifant-user))
      "/active-users"
      "/statements-per-discussion"
      "/active-users"
      "/discussions"
      "/statement-lengths"
      "/statements"
      "/usernames"))
  (testing "Unauthenticated users also have no access."
    (are [path]
      (= 401 (response-status path nil))
      "/active-users"
      "/statements-per-discussion"
      "/active-users"
      "/discussions"
      "/statement-lengths"
      "/statements"
      "/usernames"))
  (testing "Default route requires query parameter."
    (is (= 200 (response-status-main-route token-n2o-admin)))
    (is (= 403 (response-status-main-route token-schnaqqifant-user)))
    (is (= 401 (response-status-main-route nil)))))



