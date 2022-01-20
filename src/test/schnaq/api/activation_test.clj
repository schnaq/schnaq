(ns schnaq.api.activation-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.api :as api]
            [schnaq.database.activation :as activation-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def test-share-hash "cat-dog-hash")
(def test-edit-hash "cat-dog-edit-hash")

(defn- start-activation-by-share-hash [share-hash edit-hash user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/start))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)))

(deftest start-activation
  (let [share-hash test-share-hash
        edit-hash test-edit-hash
        wrong-edit-hash "wrong-edit-hash"]
    (testing "Test activation features."
      (testing "Non beta user cannot start activation."
        (is (= 403 (-> (start-activation-by-share-hash
                        share-hash
                        edit-hash
                        toolbelt/token-wegi-no-beta-user)
                       api/app :status))))
      (testing "Beta user with wrong edit hash cannot start activation."
        (is (= 403 (-> (start-activation-by-share-hash
                        share-hash
                        wrong-edit-hash
                        toolbelt/token-schnaqqifant-user)
                       api/app :status))))
      (testing "Admin and pro user can start activation."
        (is (= 200 (-> (start-activation-by-share-hash
                        share-hash
                        edit-hash
                        toolbelt/token-n2o-admin)
                       api/app :status)))))))

(deftest create-activation
  (let [share-hash test-share-hash
        edit-hash test-edit-hash]
    (testing "Test Activation creation."
      (testing "Create an Activation."
        (is (nil? (activation-db/activation-by-share-hash share-hash)))
        (is (= 200 (-> (start-activation-by-share-hash
                        share-hash
                        edit-hash
                        toolbelt/token-n2o-admin)
                       api/app :status)))
        (is (not (nil? (activation-db/activation-by-share-hash share-hash))))))))

(defn- increase-activation-by-share-hash [share-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/increase))
       :body-params {:share-hash share-hash}}
      toolbelt/add-csrf-header))

(deftest increase-activation
  (let [share-hash test-share-hash
        edit-hash test-edit-hash]
    (testing "Test Increase Activation."
      (is (= 200 (-> (start-activation-by-share-hash
                      share-hash
                      edit-hash
                      toolbelt/token-n2o-admin)
                     api/app :status)))
      (testing "Increase Counter."
        (is (= 200 (-> (increase-activation-by-share-hash
                        share-hash)
                       api/app :status)))
        (let [activation-1 (activation-db/activation-by-share-hash share-hash)]
          (is (= 1 (:activation/count activation-1))))))))

(defn- reset-activation-by-share-hash [share-hash edit-hash user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/reset))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)))

(deftest reset-activation
  (let [share-hash test-share-hash
        edit-hash test-edit-hash
        wrong-edit "wrong-edit"
        _activation-0 (activation-db/start-activation! share-hash)
        activation-1 (activation-db/increase-activation! share-hash)]
    (testing "Test reset api."
      (is (= 1 (:activation/count activation-1)))
      (testing "Non beta user cannot reset activation."
        (is (= 403 (-> (reset-activation-by-share-hash
                        share-hash
                        edit-hash
                        toolbelt/token-wegi-no-beta-user)
                       api/app :status))))
      (testing "Beta user with wrong edit hash cannot reset activation."
        (is (= 403 (-> (reset-activation-by-share-hash
                        share-hash
                        wrong-edit
                        toolbelt/token-schnaqqifant-user)
                       api/app :status))))
      (testing "Admin and beta user can start activation."
        (is (= 200 (-> (reset-activation-by-share-hash
                        share-hash
                        edit-hash
                        toolbelt/token-n2o-admin)
                       api/app :status))))
      (testing "Counter reset to 0"
        (is (= 0 (:activation/count
                  (activation-db/activation-by-share-hash share-hash))))))))
