(ns schnaq.api.activation-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.api :as api]
            [schnaq.database.activation :as activation-db]
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
      (toolbelt/mock-authorization-header user-token)))

(deftest start-activation-test
  (let [wrong-edit-hash "wrong-edit-hash"]
    (testing "Test activation features."
      (testing "Non beta user cannot start activation."
        (is (= 403 (-> (start-activation-request
                        test-share-hash
                        test-edit-hash
                        toolbelt/token-wegi-no-beta-user)
                       api/app :status))))
      (testing "Beta user with wrong edit hash cannot start activation."
        (is (= 403 (-> (start-activation-request
                        test-share-hash
                        wrong-edit-hash
                        toolbelt/token-schnaqqifant-user)
                       api/app :status))))
      (testing "Admin and pro user can start activation."
        (is (= 200 (-> (start-activation-request
                        test-share-hash
                        test-edit-hash
                        toolbelt/token-n2o-admin)
                       api/app :status)))))))

(deftest create-activation-test
  (testing "Create an Activation."
    (is (nil? (activation-db/activation-by-share-hash test-share-hash)))
    (is (= 200 (-> (start-activation-request
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-n2o-admin)
                   api/app :status)))
    (is (not (nil? (activation-db/activation-by-share-hash test-share-hash))))))

;; -----------------------------------------------------------------------------

(defn- increment-activation-request [share-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/increment))
       :body-params {:share-hash share-hash}}
      toolbelt/add-csrf-header))

(deftest increment-activation-test
  (testing "Create an activation"
    (is (= 200 (-> (start-activation-request
                    test-share-hash
                    test-edit-hash
                    toolbelt/token-n2o-admin)
                   api/app :status)))
    (testing "and increment the counter."
      (is (= 200 (-> (increment-activation-request
                      test-share-hash)
                     api/app :status)))
      (let [activation-1 (activation-db/activation-by-share-hash test-share-hash)]
        (is (= 1 (:activation/count activation-1)))))))

;; -----------------------------------------------------------------------------

(defn- reset-activation-by-share-hash [share-hash edit-hash user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :activation/reset))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)))

(deftest reset-activation-test
  (let [wrong-edit "wrong-edit"
        _activation-0 (activation-db/start-activation! test-share-hash)
        activation-1 (activation-db/increment-activation! test-share-hash)]
    (testing "Test reset api."
      (is (= 1 (:activation/count activation-1)))
      (testing "Non beta user cannot reset activation."
        (is (= 403 (-> (reset-activation-by-share-hash
                        test-share-hash
                        test-edit-hash
                        toolbelt/token-wegi-no-beta-user)
                       api/app :status))))
      (testing "Beta user with wrong edit hash cannot reset activation."
        (is (= 403 (-> (reset-activation-by-share-hash
                        test-share-hash
                        wrong-edit
                        toolbelt/token-schnaqqifant-user)
                       api/app :status))))
      (testing "Admin and beta user can start activation."
        (is (= 200 (-> (reset-activation-by-share-hash
                        test-share-hash
                        test-edit-hash
                        toolbelt/token-n2o-admin)
                       api/app :status)))
        (is (= 200 (-> (reset-activation-by-share-hash
                        test-share-hash
                        test-edit-hash
                        toolbelt/token-schnaqqifant-user)
                       api/app :status))))
      (testing "Counter reset to 0"
        (is (= 0 (:activation/count
                  (activation-db/activation-by-share-hash test-share-hash))))))))
