(ns schnaq.database.access-codes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [schnaq.database.access-codes :as ac]))

(deftest generate-schnaq-access-code-test
  (testing "Valid codes are generated."
    (is (s/valid? :discussion.access/code (#'ac/generate-access-code)))))
