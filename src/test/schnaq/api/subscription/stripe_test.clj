(ns schnaq.api.subscription.stripe-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.api :as api]
            [schnaq.api.subscription.stripe :as stripe]
            [schnaq.config.stripe :refer [prices]]
            [schnaq.test.toolbelt :as toolbelt :refer [token-n2o-admin test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- request [verb route-name body-params]
  (-> {:request-method verb
       :uri (:template (api/route-by-name route-name))
       :body-params body-params}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token-n2o-admin)
      test-app))

(deftest cancel-subscription-test
  (testing "Test users have no valid subscription and can't cancel it."
    (is (= 400 (:status (request :post :api.stripe/cancel-user-subscription {:cancel? true}))))))

;; -----------------------------------------------------------------------------

(deftest get-product-prices-test
  (let [get-product-prices #'stripe/get-product-prices
        yearly-price-id-eur (get-in prices [:eur :schnaq.pro/yearly])
        yearly-price-id-usd (get-in prices [:usd :schnaq.pro/yearly])
        response (get-product-prices {})]
    (testing "Price retrieval should query prices from stripe."
      (is (= yearly-price-id-eur (get-in response
                                         [:body :prices :eur :schnaq.pro/yearly :id])))
      (is (= yearly-price-id-usd (get-in response
                                         [:body :prices :usd :schnaq.pro/yearly :id]))))))
