(ns schnaq.api.subscription.stripe-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.api :as api]
            [schnaq.api.subscription.stripe :as stripe]
            [schnaq.config.stripe :refer [prices]]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as toolbelt :refer [token-n2o-admin]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- request [verb route-name body-params]
  (-> {:request-method verb
       :uri (:template (api/route-by-name route-name))
       :body-params body-params}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token-n2o-admin)
      api/app))

(deftest cancel-subscription-test
  (testing "Test users have no valid subscription and can't cancel it."
    (is (= 400 (:status (request :post :api.stripe/cancel-user-subscription {:cancel? true}))))))

;; -----------------------------------------------------------------------------
;; Testing webhook events

(def webhook #'stripe/webhook)
(def keycloak-id (:user.registered/keycloak-id kangaroo))

(deftest webhook-customer-subscription-created-test
  (let [subscription-id "sub_foo"]
    (testing "Store subscription information from stripe into the database."
      (is (= 200 (:status (webhook {:body-params {:type "customer.subscription.created"
                                                  :data {:object {:metadata {:keycloak-id keycloak-id}
                                                                  :customer "cus_foo"
                                                                  :id subscription-id}}}}))))
      (is (= subscription-id
             (:user.registered.subscription/stripe-id
              (user-db/private-user-by-keycloak-id keycloak-id)))))))

;; -----------------------------------------------------------------------------

(defn get-product-price-call [price-id]
  (#'stripe/get-product-price {:parameters {:query {:price-id price-id}}}))

(deftest get-product-price
  (let [price-id (:schnaq.pro/monthly prices)]
    (testing "Price retrieval"
      (testing "is successful if article can be found."
        (is (= price-id (get-in (get-product-price-call price-id)
                                [:body :id]))))
      (testing "fails if article can't be found."
        (is (= :stripe.price/invalid-request (get-in (get-product-price-call "price_foo")
                                                     [:body :error])))))))
