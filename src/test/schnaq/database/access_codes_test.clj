(ns schnaq.database.access-codes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [schnaq.database.access-codes :as ac]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest generate-code-test
  (testing "Valid codes are generated."
    (is (s/valid? :discussion.access/code (#'ac/generate-code)))))

(def sample {:db/id 17592186045451,
             :discussion.access/code 42,
             :discussion.access/discussion [:discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0"]
             :discussion.access/created-at #inst"2021-10-06T10:56:36.257-00:00",
             :discussion.access/expires-at #inst"2021-10-07T12:56:36.257-00:00"})

(deftest valid?-test
  (let [valid? #'ac/valid?]
    (testing "Valid access-code with all fields is okay."
      (is (valid? sample)))
    (testing "Missing discussion or missing code is invalid."
      (is (not (valid? (dissoc sample :discussion.access/discussion))))
      (is (not (valid? (dissoc sample :discussion.access/code)))))
    (testing "If expired is smaller than created, the access code is invalid."
      (is (not (valid? (assoc sample :discussion.access/expires-at #inst"2000-10-07T12:56:36.257-00:00"))))
      (is (valid? (assoc sample :discussion.access/expires-at #inst"2021-10-06T10:56:36.257-00:00"))))))

(deftest code-available?-test
  (let [{:discussion.access/keys [code]} (ac/add-access-code-to-discussion "cat-dog-hash" 42)
        code-available? #'ac/code-available?]
    (testing "Verify, that the code is really available."
      (is (not (code-available? code)))
      (is (code-available? 23232323)))))

