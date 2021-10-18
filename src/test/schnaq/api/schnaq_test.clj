(ns schnaq.api.schnaq-test
  (:require [clojure.test :refer [deftest is are testing use-fixtures]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- schnaq-by-hash-as-admin-request [share-hash edit-hash]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.schnaq/by-hash-as-admin))
       :body-params {:share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      api/app))

(deftest schnaq-by-hash-as-admin-test
  (let [share-hash "graph-hash"
        edit-hash "graph-edit-hash"]
    (testing "Valid hashes are ok."
      (is (= 200 (:status (schnaq-by-hash-as-admin-request share-hash edit-hash)))))
    (testing "Wrong hashes are forbidden."
      (is (= 403 (:status (schnaq-by-hash-as-admin-request share-hash "👾"))))
      (is (= 403 (:status (schnaq-by-hash-as-admin-request "razupaltuff" edit-hash)))))))

(defn- schnaqs-by-hashes-request [share-hashes]
  (-> {:request-method :get :uri (:path (api/route-by-name :api.schnaqs/by-hashes))
       :headers {"accept" "application/edn"}
       :query-params {:share-hashes share-hashes
                      :display-name "Anonymous"}}
      api/app))

(deftest schnaqs-by-hashes-test
  (let [share-hash1 "cat-dog-hash"
        share-hash2 "graph-hash"]
    (testing "No hash provided is a bad request."
      (is (= 400 (:status (schnaqs-by-hashes-request nil)))))
    (testing "Invalid hash returns no discussion."
      (is (= 200 (:status (schnaqs-by-hashes-request "something-non-existent"))))
      (is (empty? (:schnaqs (m/decode-response-body (schnaqs-by-hashes-request "something-non-existent"))))))
    (testing "Querying by a single valid hash returns a discussion."
      (let [api-call (schnaqs-by-hashes-request share-hash1)]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (:schnaqs (m/decode-response-body api-call)))))))
    (testing "A valid hash packed into a collection should also work."
      (let [api-call (schnaqs-by-hashes-request [share-hash1])]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (:schnaqs (m/decode-response-body api-call)))))))
    (testing "Asking for multiple valid hashes, returns a list of valid discussions."
      (let [api-call (schnaqs-by-hashes-request [share-hash1 share-hash2])]
        (is (= 200 (:status api-call)))
        (is (= 2 (count (:schnaqs (m/decode-response-body api-call)))))))))

(defn- add-schnaq-request [payload]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.schnaq/add))
       :body-params payload}
      toolbelt/add-csrf-header
      toolbelt/accept-edn-response-header
      (toolbelt/mock-authorization-header toolbelt/token-schnaqqifant-user)
      api/app))

(deftest add-schnaq-test
  (testing "schnaq creation."
    (let [minimal-request {:discussion-title "huhu"}]
      (are [status payload]
        (= status (:status (add-schnaq-request payload)))
        400 {}
        400 {:nickname "penguin"}
        400 {:razupaltuff "kangaroo"}
        201 minimal-request
        201 (merge minimal-request {:hub-exclusive? true})
        201 (merge minimal-request {:hub-exclusive? false})
        201 (merge minimal-request {:hub-exclusive? false
                                    :hub "works, because we don't provide error message"})
        201 (merge minimal-request {:ends-in-days 42})
        201 (merge minimal-request {:discussion-mode :discussion.mode/qanda})))))
