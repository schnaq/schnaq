(ns schnaq.links-test
  (:require [clojure.test :refer [deftest is testing]]
            [schnaq.links :as links]))

(deftest legal-pages-test
  (testing "The privacy policy on schnaq.app follows the locale and falls back to German."
    (is (= "https://schnaq.app/en/privacy/" (links/privacy-policy :en)))
    (is (= "https://schnaq.app/de/privacy/" (links/privacy-policy :de)))
    (is (= "https://schnaq.app/de/privacy/" (links/privacy-policy nil))))
  (testing "The imprint on schnaq.com follows the locale as well."
    (is (= "https://schnaq.com/en/legal-note" (links/legal-note :en)))
    (is (= "https://schnaq.com/de/legal-note" (links/legal-note :de)))))
