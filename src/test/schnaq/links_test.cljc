(ns schnaq.links-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.links :as links]))

(deftest privacy-policy-test
  (testing "The privacy policy on schnaq.app follows the locale and falls back to German."
    (is (= "https://schnaq.app/en/privacy/" (links/privacy-policy :en)))
    (is (= "https://schnaq.app/de/privacy/" (links/privacy-policy :de)))
    (is (= "https://schnaq.app/de/privacy/" (links/privacy-policy nil)))))
