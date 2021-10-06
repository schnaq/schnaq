(ns schnaq.database.access-codes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [schnaq.database.access-codes :as ac]))

(deftest generate-code-test
  (testing "Valid codes are generated."
    (is (s/valid? :discussion.access/code (#'ac/generate-code)))))

(def sample {:db/id 17592186045451,
             :discussion.access/code 42,
             :discussion.access/discussion [:discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0"]
             :discussion.access/created-at #inst"2021-10-06T10:56:36.257-00:00",
             :discussion.access/expires-at #inst"2021-10-07T12:56:36.257-00:00"})

(deftest valid?-test
  (testing "Valid access-code with all fields is okay."
    (is (ac/valid? sample)))
  (testing "Missing discussion or missing code is invalid."
    (is (not (ac/valid? (dissoc sample :discussion.access/discussion))))
    (is (not (ac/valid? (dissoc sample :discussion.access/code)))))
  (testing "If expired is smaller than created, the access code is invalid."
    (is (not (ac/valid? (assoc sample :discussion.access/expires-at #inst"2000-10-07T12:56:36.257-00:00"))))
    (is (ac/valid? (assoc sample :discussion.access/expires-at #inst"2021-10-06T10:56:36.257-00:00")))))
