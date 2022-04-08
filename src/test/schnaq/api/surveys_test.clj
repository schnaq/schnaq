(ns schnaq.api.surveys-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- using-schnaq-for-request [user-token topics]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.surveys.participate/using-schnaq-for))
       :body-params {:topics topics}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      :status))

(deftest using-schnaq-for-test
  (is (= 200 (using-schnaq-for-request toolbelt/token-kangaroo-normal-user
                                       [:surveys.using-schnaq-for.topics/meetings])))
  (is (= 200 (using-schnaq-for-request nil
                                       [:surveys.using-schnaq-for.topics/meetings]))))
