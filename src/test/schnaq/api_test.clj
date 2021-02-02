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

(deftest add-meeting-with-empty-description-test
  (testing "Check whether a meeting with an empty description is added or refused."
    (let [response (@#'api/add-meeting {:body-params {:meeting {:meeting/title "Test"
                                                                :meeting/start-date (db/now)
                                                                :meeting/end-date (db/now)
                                                                :meeting/description ""}
                                                      :nickname "Wegi"
                                                      :public-discussion? true}})]
      (is (= 201 (:status response)))
      (is (s/valid? ::specs/meeting (-> response :body :new-meeting))))))

(deftest check-credentials-test
  (testing "Check if credentials are verified correctly."
    (let [check-credentials @#'api/check-credentials
          share-hash "abbada"
          edit-hash "Scooby Doo"
          _ (db/add-meeting {:meeting/title "foo"
                             :meeting/share-hash share-hash
                             :meeting/edit-hash edit-hash
                             :meeting/start-date (db/now)
                             :meeting/end-date (db/now)
                             :meeting/author (db/add-user-if-not-exists "Wegi")})
          succeeding-response (check-credentials {:body-params {:share-hash share-hash :edit-hash edit-hash}})
          failing-response (check-credentials {:body-params {:share-hash share-hash :edit-hash "INVALID"}})]
      (is (= 200 (:status succeeding-response)))
      (is (-> succeeding-response :body :valid-credentials?))
      (is (not (-> failing-response :body :valid-credentials?)))
      (is (= 200 (:status failing-response))))))

(deftest graph-data-for-agenda-test
  (testing "Check if graph data is correct"
    (let [graph-data-for-agenda @#'api/graph-data-for-agenda
          share-hash "89eh32hoas-2983ud"
          discussion-id (:db/id (first (discussion-db/all-discussions-by-title "Cat or Dog?")))
          request {:body-params {:share-hash share-hash
                                 :discussion-id discussion-id}}
          bad-request {:body-params {:share-hash "123"
                                     :discussion-id 456}}
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

(deftest meeting-by-hash-as-admin-test
  (let [meeting-by-hash-as-admin #'api/meeting-by-hash-as-admin
        share-hash "graph-hash"
        edit-hash "graph-edit-hash"
        request {:body-params {:share-hash share-hash
                               :edit-hash edit-hash}}
        req-wrong-edit-hash {:body-params {:share-hash share-hash
                                           :edit-hash "👾"}}
        req-wrong-share-hash {:body-params {:share-hash "razupaltuff"
                                            :edit-hash edit-hash}}]
    (testing "Valid hashes are ok."
      (is (= 200 (:status (meeting-by-hash-as-admin request)))))
    (testing "Wrong hashes are forbidden."
      (is (= 403 (:status (meeting-by-hash-as-admin req-wrong-edit-hash))))
      (is (= 403 (:status (meeting-by-hash-as-admin req-wrong-share-hash)))))))

(deftest meetings-by-hashes-test
  (let [meetings-by-hashes #'api/meetings-by-hashes
        share-hash1 "89eh32hoas-2983ud"
        share-hash2 "graph-hash"]
    (testing "No hash provided, no meeting returned."
      (is (= 400 (:status (meetings-by-hashes {})))))
    (testing "Invalid hash returns no meeting."
      (is (= 404 (:status (meetings-by-hashes
                            {:params {:share-hashes "something-non-existent"}})))))
    (testing "Querying by a single valid hash returns a meeting."
      (let [api-call (meetings-by-hashes {:params {:share-hashes share-hash1}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :meetings]))))
        (is (s/valid? ::specs/meeting (first (get-in api-call [:body :meetings]))))))
    (testing "A valid hash packed into a collection should also work."
      (let [api-call (meetings-by-hashes {:params {:share-hashes [share-hash1]}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :meetings]))))
        (is (s/valid? ::specs/meeting (first (get-in api-call [:body :meetings]))))))
    (testing "Asking for multiple valid hashes, returns a list of valid meetings."
      (let [api-call (meetings-by-hashes {:params {:share-hashes [share-hash1 share-hash2]}})]
        (is (= 200 (:status api-call)))
        (is (= 2 (count (get-in api-call [:body :meetings]))))
        (is (every? true?
                    (map (partial s/valid? ::specs/meeting)
                         (get-in api-call [:body :meetings]))))))))
