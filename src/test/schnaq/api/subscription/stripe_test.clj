(ns schnaq.api.subscription.stripe-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt :refer [token-n2o-admin]]))

(defn- request [verb route-name body-params]
  (-> {:request-method verb
       :uri (:template (api/route-by-name route-name))
       :body-params body-params}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token-n2o-admin)))

(deftest cancel-subscription-test
  (testing "Test users have no valid subscription and can't cancel it."
    (is (= 400 (-> (request :post :api.stripe/cancel-user-subscription {:cancel? true})
                   api/app
                   :status)))))
