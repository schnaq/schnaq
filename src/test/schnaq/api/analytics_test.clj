(ns schnaq.api.analytics-test
  (:require [clojure.test :refer [use-fixtures deftest is are testing]]
            [ring.mock.request :refer [request]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt :refer [token-schnaqqifant-user token-n2o-admin]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- response-status [path token]
  (-> (request :get (format "/admin/analytics%s" path))
      (toolbelt/mock-authorization-header token)
      api/app
      :status))

(defn- response-status-main-route [token]
  (-> {:request-method :get :uri "/admin/analytics" :query-params {:days-since 7}}
      (toolbelt/mock-authorization-header token)
      api/app
      :status))

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



