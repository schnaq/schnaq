(ns schnaq.api-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is are use-fixtures]]
            [ring.mock.request :as mock]
            [schnaq.api :as api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest check-credentials-test
  (testing "Check if credentials are verified correctly."
    (let [check-credentials @#'api/check-credentials
          share-hash "abbada"
          edit-hash "Scooby Doo"
          _ (discussion-db/new-discussion {:discussion/title "foo"
                                           :discussion/share-hash share-hash
                                           :discussion/edit-hash edit-hash
                                           :discussion/author (user-db/add-user-if-not-exists "Wegi")}
                                          true)
          succeeding-response (check-credentials {:params {:share-hash share-hash :edit-hash edit-hash}})
          failing-response (check-credentials {:params {:share-hash share-hash :edit-hash "INVALID"}})]
      (is (= 200 (:status succeeding-response)))
      (is (-> succeeding-response :body :valid-credentials?))
      (is (not (-> failing-response :body :valid-credentials?)))
      (is (= 200 (:status failing-response))))))

(deftest graph-data-for-agenda-test
  (testing "Check if graph data is correct"
    (let [graph-data-for-agenda @#'api/graph-data-for-agenda
          share-hash "cat-dog-hash"
          request {:body-params {:share-hash share-hash}}
          bad-request {:body-params {:share-hash "123"}}
          response (graph-data-for-agenda request)
          bad-response (graph-data-for-agenda bad-request)
          error-text "Invalid meeting hash. You are not allowed to view this data."]
      (testing "valid request"
        (is (= 200 (:status response)))
        (is (contains? (:body response) :graph))
        (is (contains? (-> response :body :graph) :nodes))
        (is (contains? (-> response :body :graph) :edges))
        (is (not (nil? (-> response :body :graph :nodes))))
        (is (not (nil? (-> response :body :graph :edges)))))
      (testing "bad request"
        (is (= 400 (:status bad-response)))
        (is (= error-text (-> bad-response :body :error)))))))

(deftest cors-origin-tests
  (let [test-regex (partial re-matches api/allowed-origin)]
    (testing "Valid origins for production mode."
      (are [origin] (not (nil? (test-regex origin)))
                    "api.schnaq.com"
                    "schnaq.com"
                    "schnaq.de"
                    "www.schnaq.de"
                    "www.schnaq.com"
                    "https://api.schnaq.com"
                    "https://schnaq.com"
                    "https://schnaq.com/?kangaroo=rocks"
                    "api.staging.schnaq.com"
                    "staging.schnaq.com"
                    "https://api.staging.schnaq.com"
                    "https://staging.schnaq.com"
                    "https://staging.schnaq.com/meetings/create"))
    (testing "Invalid origins."
      (are [origin] (nil? (test-regex origin))
                    "localhost"
                    "penguin.books"
                    "christian.rocks"
                    "schnaqqi.com"
                    "schnaq.dev"
                    "fakeschnaq.com"))))

(deftest schnaq-by-hash-as-admin-test
  (let [schnaq-by-hash-as-admin #'api/schnaq-by-hash-as-admin
        share-hash "graph-hash"
        edit-hash "graph-edit-hash"
        request {:body-params {:share-hash share-hash
                               :edit-hash edit-hash}}
        req-wrong-edit-hash {:body-params {:share-hash share-hash
                                           :edit-hash "👾"}}
        req-wrong-share-hash {:body-params {:share-hash "razupaltuff"
                                            :edit-hash edit-hash}}]
    (testing "Valid hashes are ok."
      (is (= 200 (:status (schnaq-by-hash-as-admin request)))))
    (testing "Wrong hashes are forbidden."
      (is (= 403 (:status (schnaq-by-hash-as-admin req-wrong-edit-hash))))
      (is (= 403 (:status (schnaq-by-hash-as-admin req-wrong-share-hash)))))))

(deftest schnaqs-by-hashes-test
  (let [schnaqs-by-hashes #'api/schnaqs-by-hashes
        share-hash1 "cat-dog-hash"
        share-hash2 "graph-hash"]
    (testing "No hash provided, no discussion returned."
      (is (= 400 (:status (schnaqs-by-hashes {})))))
    (testing "Invalid hash returns no discussion."
      (is (= 200 (:status (schnaqs-by-hashes
                            {:params {:share-hashes "something-non-existent"}}))))
      (is (empty? (get-in (schnaqs-by-hashes {:params {:share-hashes "something-non-existent"}})
                          [:body :discussions]))))
    (testing "Querying by a single valid hash returns a discussion."
      (let [api-call (schnaqs-by-hashes {:params {:share-hashes share-hash1}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :discussions]))))
        (is (s/valid? ::specs/discussion (first (get-in api-call [:body :discussions]))))))
    (testing "A valid hash packed into a collection should also work."
      (let [api-call (schnaqs-by-hashes {:params {:share-hashes [share-hash1]}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :discussions]))))
        (is (s/valid? ::specs/discussion (first (get-in api-call [:body :discussions]))))))
    (testing "Asking for multiple valid hashes, returns a list of valid discussions."
      (let [api-call (schnaqs-by-hashes {:params {:share-hashes [share-hash1 share-hash2]}})]
        (is (= 200 (:status api-call)))
        (is (= 2 (count (get-in api-call [:body :discussions]))))
        (is (every? true?
                    (map (partial s/valid? ::specs/discussion)
                         (get-in api-call [:body :discussions]))))))))

(deftest edit-statement!-test
  (let [edit-statement! #'api/edit-statement!
        share-hash "simple-hash"
        keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"
        statement (first (discussion-db/starting-statements share-hash))
        request #(-> (mock/request :put "/discussion/statement/edit")
                     (assoc-in [:identity :sub] %)
                     (assoc-in [:params :share-hash] share-hash)
                     (assoc-in [:params :statement-id] (:db/id statement))
                     (assoc-in [:params :new-content] "any-text"))]
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
