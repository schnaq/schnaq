(ns schnaq.database.access-codes-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is]]
            [schnaq.database.access-codes :as ac]
            [schnaq.database.specs :as specs]))

(deftest generate-schnaq-access-code-test
  (testing "Valid codes are generated."
    (is (s/valid? ::specs/access-code (#'ac/generate-access-code)))))
