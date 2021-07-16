(ns schnaq.api.schnaq-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.api.schnaq :as schnaq-api]
            [schnaq.database.specs :as specs]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest schnaq-by-hash-as-admin-test
  (let [schnaq-by-hash-as-admin #'schnaq-api/schnaq-by-hash-as-admin
        share-hash "graph-hash"
        edit-hash "graph-edit-hash"
        request {:parameters {:body {:share-hash share-hash
                                     :edit-hash edit-hash}}}
        req-wrong-edit-hash {:parameters {:body {:share-hash share-hash
                                                 :edit-hash "👾"}}}
        req-wrong-share-hash {:parameters {:body {:share-hash "razupaltuff"
                                                  :edit-hash edit-hash}}}]
    (testing "Valid hashes are ok."
      (is (= 200 (:status (schnaq-by-hash-as-admin request)))))
    (testing "Wrong hashes are forbidden."
      (is (= 403 (:status (schnaq-by-hash-as-admin req-wrong-edit-hash))))
      (is (= 403 (:status (schnaq-by-hash-as-admin req-wrong-share-hash)))))))

(deftest schnaqs-by-hashes-test
  (let [schnaqs-by-hashes #'schnaq-api/schnaqs-by-hashes
        share-hash1 "cat-dog-hash"
        share-hash2 "graph-hash"]
    (testing "No hash provided, no discussion returned."
      (is (= 404 (:status (schnaqs-by-hashes {})))))
    (testing "Invalid hash returns no discussion."
      (is (= 200 (:status (schnaqs-by-hashes
                            {:parameters {:query {:share-hashes "something-non-existent"}}}))))
      (is (empty? (get-in (schnaqs-by-hashes {:parameters {:query {:share-hashes "something-non-existent"}}})
                          [:body :schnaqs]))))
    (testing "Querying by a single valid hash returns a discussion."
      (let [api-call (schnaqs-by-hashes {:parameters {:query {:share-hashes share-hash1}}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :schnaqs]))))
        (is (s/valid? ::specs/discussion (first (get-in api-call [:body :schnaqs]))))))
    (testing "A valid hash packed into a collection should also work."
      (let [api-call (schnaqs-by-hashes {:parameters {:query {:share-hashes [share-hash1]}}})]
        (is (= 200 (:status api-call)))
        (is (= 1 (count (get-in api-call [:body :schnaqs]))))
        (is (s/valid? ::specs/discussion (first (get-in api-call [:body :schnaqs]))))))
    (testing "Asking for multiple valid hashes, returns a list of valid discussions."
      (let [api-call (schnaqs-by-hashes {:parameters {:query {:share-hashes [share-hash1 share-hash2]}}})]
        (is (= 200 (:status api-call)))
        (is (= 2 (count (get-in api-call [:body :schnaqs]))))
        (is (every? true?
                    (map (partial s/valid? ::specs/discussion)
                         (get-in api-call [:body :schnaqs]))))))))