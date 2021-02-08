(ns schnaq.api.analytics-test
  (:require [clojure.test :refer [deftest are testing]]
            [ring.mock.request :as mock]
            [schnaq.api.analytics :as sut]
            [schnaq.test.toolbelt :refer [token-schnaqqifant-user token-n2o-admin mock-authorization-header]]))

(defn- response-status [path token]
  (:status (sut/analytics-routes
             (-> (mock/request :get (format "/analytics%s" path))
                 (mock-authorization-header token)))))

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
      "/usernames")))



