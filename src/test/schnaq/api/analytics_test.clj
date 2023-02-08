(ns schnaq.api.analytics-test
  (:require [clojure.test :refer [use-fixtures deftest is testing]]
            [muuntaja.core :as m]
            [schnaq.test.toolbelt :as toolbelt :refer [token-schnaqqifant-user token-n2o-admin test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- response-status-main-route [token]
  (-> {:request-method :get :uri "/admin/analytics" :query-params {:days-since 7}}
      (toolbelt/mock-authorization-header token)
      test-app
      :status))

(deftest permission-test
  (testing "Default route requires query parameter."
    (is (= 200 (response-status-main-route token-n2o-admin)))
    (is (= 403 (response-status-main-route token-schnaqqifant-user)))
    (is (= 401 (response-status-main-route nil)))))

;; -----------------------------------------------------------------------------

(defn- request-pattern-statistics [token patterns]
  (-> {:request-method :get :uri "/admin/analytics/by-emails" :query-params {:patterns patterns}}
      (toolbelt/mock-authorization-header token)
      test-app
      m/decode-response-body))

(deftest get-statistics-by-email-test
  (is (:statistics (request-pattern-statistics token-n2o-admin ".*@schnaq\\.com")))
  (is (nil? (:statistics (request-pattern-statistics token-n2o-admin "nothing-to-see-here"))))
  (is (:error (request-pattern-statistics token-schnaqqifant-user "foo")))
  (is (:error (request-pattern-statistics nil "foo"))))
