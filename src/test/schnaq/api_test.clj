(ns schnaq.api-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is are use-fixtures]]
            [schnaq.api :as api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as db]
            [schnaq.meeting.specs :as specs]
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
                                           :discussion/author (db/add-user-if-not-exists "Wegi")}
                                          true)
          succeeding-response (check-credentials {:body-params {:share-hash share-hash :edit-hash edit-hash}})
          failing-response (check-credentials {:body-params {:share-hash share-hash :edit-hash "INVALID"}})]
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
                    "schnaqi.com"
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
