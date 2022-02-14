(ns schnaq.shared-toolbelt-test
  (:require
   [clojure.test :refer [deftest are testing]]
   [schnaq.shared-toolbelt :as tools]))

(deftest slugify-test
  (testing "Slugs should contain no whitespace or other special characters."
    (are [slugged input] (= slugged (tools/slugify input))
      "kangaroo" "kangaroo"
      "kangaroo" "Kangaroo"
      "penguin-books" "Penguin Books"
      "" "")))