(ns schnaq.api.activation-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [use-fixtures is deftest testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.activation :as activation-db]
            [schnaq.database.specs :as specs]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def test-share-hash "cat-dog-hash")
(def test-edit-hash "cat-dog-edit-hash")

(defn- start-activation-request [share-hash edit-hash user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/start))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      api/app
      :status))

(deftest start-activation-test
  (let [wrong-edit-hash "wrong-edit-hash"]
    (testing "Test activation features."
      (testing "Non beta user cannot start activation."
        (is (= 403 (start-activation-request
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-wegi-no-beta-user))))
      (testing "Beta user with wrong edit hash cannot start activation."
        (is (= 403 (start-activation-request
                    test-share-hash
                    wrong-edit-hash
                    toolbelt/token-schnaqqifant-user))))
      (testing "Admin and pro user can start activation."
        (is (= 200 (start-activation-request
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-n2o-admin)))))))

(deftest create-activation-test
  (testing "Create an Activation."
    (is (nil? (activation-db/activation-by-share-hash test-share-hash)))
    (is (= 200 (start-activation-request
                test-share-hash
                test-edit-hash
                toolbelt/token-n2o-admin)))
    (is (not (nil? (activation-db/activation-by-share-hash test-share-hash))))))

;; -----------------------------------------------------------------------------

(defn- get-activation-request [share-hash]
  (-> {:request-method :get :uri (:path (api/route-by-name :activation/get))
       :query-params {:share-hash share-hash}}
      api/app))

(deftest get-activation-no-activation-available-test
  (testing "If no activation is active, the request should be empty."
    (let [response (get-activation-request test-share-hash)]
      (is (nil? (-> response
                    m/decode-response-body
                    :activation))))))

(deftest get-activation-with-activation-test
  (testing "If there is an activation, return it."
    (start-activation-request test-share-hash test-edit-hash toolbelt/token-n2o-admin)
    (let [response (get-activation-request test-share-hash)]
      (is (-> response
              m/decode-response-body
              :activation
              (partial s/valid? ::specs/activation)))
      (is (= 200 (:status (get-activation-request test-share-hash)))))))

;; -----------------------------------------------------------------------------

(defn- increment-activation-request [share-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/increment))
       :body-params {:share-hash share-hash}}
      toolbelt/add-csrf-header
      api/app
      :status))

(deftest increment-activation-test
  (testing "Create an activation"
    (is (= 200 (start-activation-request
                test-share-hash
                test-edit-hash
                toolbelt/token-n2o-admin)))
    (testing "and increment the counter."
      (is (= 200 (increment-activation-request
                  test-share-hash)))
      (let [activation-1 (activation-db/activation-by-share-hash test-share-hash)]
        (is (= 1 (:activation/count activation-1)))))))

;; -----------------------------------------------------------------------------

(defn- reset-activation-by-share-hash [share-hash edit-hash user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/reset))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      api/app
      :status))

(deftest reset-activation-test
  (let [wrong-edit "wrong-edit"
        _activation-0 (activation-db/start-activation! test-share-hash)
        activation-1 (activation-db/increment-activation! test-share-hash)]
    (testing "Test reset api."
      (is (= 1 (:activation/count activation-1)))
      (testing "Non beta user cannot reset activation."
        (is (= 403 (reset-activation-by-share-hash
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-wegi-no-beta-user))))
      (testing "Beta user with wrong edit hash cannot reset activation."
        (is (= 403 (reset-activation-by-share-hash
                    test-share-hash
                    wrong-edit
                    toolbelt/token-schnaqqifant-user))))
      (testing "Admin and beta user can start activation."
        (is (= 200 (reset-activation-by-share-hash
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-n2o-admin)))
        (is (= 200 (reset-activation-by-share-hash
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-schnaqqifant-user))))
      (testing "Counter reset to 0"
        (is (= 0 (:activation/count
                  (activation-db/activation-by-share-hash test-share-hash))))))))
