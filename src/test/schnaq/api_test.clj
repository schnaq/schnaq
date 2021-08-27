(ns schnaq.api-test
  (:require [clojure.test :refer [deftest testing is are use-fixtures]]
            [ring.middleware.cors :as cors]
            [ring.mock.request :as mock]
            [schnaq.api :as api]
            [schnaq.api.discussion :as discussion-api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]
            [schnaq.toolbelt :as toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest check-credentials-test
  (testing "Check if credentials are verified correctly."
    (let [credential-request (fn [share-hash edit-hash]
                               (schnaq-toolbelt/add-csrf-header
                                 {:request-method :post :uri "/credentials/validate"
                                  :body-params {:share-hash share-hash :edit-hash edit-hash}}))
          share-hash "simple-hash"
          edit-hash "simple-hash-secret"]
      (is (= 200 (-> (credential-request share-hash edit-hash) api/app :status)))
      (is (= 403 (-> (credential-request "invalid" edit-hash) api/app :status)))
      (is (= 403 (-> (credential-request share-hash "invalid") api/app :status)))
      (is (= 403 (-> (credential-request "invalid" "invalid") api/app :status))))))

(deftest graph-data-for-agenda-test
  (testing "Check if graph data is correct"
    (let [graph-data-for-agenda @#'discussion-api/graph-data-for-agenda
          graph-request (fn [share-hash] (graph-data-for-agenda {:parameters {:query {:share-hash share-hash}}}))
          share-hash "cat-dog-hash"
          valid-response (graph-request "cat-dog-hash")]
      (testing "valid request"
        (is (= 200 (:status valid-response)))
        (is (contains? (-> valid-response :body) :graph))
        (is (contains? (-> valid-response :body :graph) :nodes))
        (is (contains? (-> valid-response :body :graph) :edges))
        (is (not (nil? (-> valid-response :body :graph :nodes))))
        (is (not (nil? (-> valid-response :body :graph :edges)))))
      (testing "Check with complete app"
        (is (= 200 (:status (api/app {:request-method :get :uri "/discussion/graph"
                                      :query-params {:share-hash share-hash}}))))
        (is (= 404 (:status (api/app {:request-method :get :uri "/discussion/graph"
                                      :query-params {:share-hash "bad-hash"}}))))))))

(deftest api-cors-test
  (testing "CORS settings for main API."
    (are [origin expected]
      (= expected (cors/allow-request?
                    {:headers {"origin" origin}
                     :request-method :get}
                    {:access-control-allow-origin (conj api/allowed-origins (toolbelt/build-allowed-origin "schnaq.localhost"))
                     :access-control-allow-methods api/allowed-http-verbs}))
      nil false
      "" false
      "http://schnaq.com" true
      "https://schnaq.com" true
      "api.schnaq.com" true
      "schnaq.com" true
      "schnaq.de" true
      "www.schnaq.de" true
      "www.schnaq.com" true
      "https://api.schnaq.com" true
      "https://schnaq.com" true
      "https://schnaq.com/?kangaroo=rocks" true
      "api.staging.schnaq.com" true
      "staging.schnaq.com" true
      "https://api.staging.schnaq.com" true
      "https://staging.schnaq.com" true
      "https://staging.schnaq.com/schnaq/create" true
      "http://schnaq.localhost" true
      "https://schnaq.localhost" true
      "https://schnaq.localhost/schnaqs" true
      "https://schnaq.localhost/schnaqs/public" true
      "localhost" false
      "penguin.books" false
      "christian.rocks" false
      "schnaqqi.com" false
      "schnaq.dev" false
      "fakeschnaq.com" false
      "http://schnaqqifantenparty.com" false
      "https://schnaqqifantenparty.com" false)))

(deftest edit-statement!-test
  (let [edit-statement! #'discussion-api/edit-statement!
        share-hash "simple-hash"
        keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"
        statement (first (discussion-db/starting-statements share-hash))
        request #(-> (mock/request :put "/discussion/statement/edit")
                     (assoc-in [:identity :sub] %)
                     (assoc-in [:parameters :body :share-hash] share-hash)
                     (assoc-in [:parameters :body :statement-type] :statement.type/neutral)
                     (assoc-in [:parameters :body :statement-id] (:db/id statement))
                     (assoc-in [:parameters :body :new-content] "any-text"))]
    (testing "Only requests from valid author should be allowed."
      ;; The author is not the registered user, rest is fine
      (is (= 403 (:status (edit-statement! (request keycloak-id)))))
      ;; Make the author the user
      (db/transact [[:db/add (:db/id statement) :statement/author [:user.registered/keycloak-id keycloak-id]]])
      ;; Everything should be fine
      (is (= 200 (:status (edit-statement! (request keycloak-id)))))
      ;; Statement is deleted
      (db/transact [[:db/add (:db/id statement) :statement/deleted? true]])
      (is (= 400 (:status (edit-statement! (request keycloak-id)))))
      ;; Statement is fine but discussion is read-only
      (db/transact [[:db/add (:db/id statement) :statement/deleted? false]])
      (discussion-db/set-discussion-read-only share-hash)
      (is (= 400 (:status (edit-statement! (request keycloak-id))))))))
