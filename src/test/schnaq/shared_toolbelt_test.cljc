(ns schnaq.shared-toolbelt-test
  (:require [clojure.test :refer [deftest is are testing]]
            [schnaq.shared-toolbelt :as tools]))

(deftest slugify-test
  (testing "Slugs should contain no whitespace or other special characters."
    (are [slugged input] (= slugged (tools/slugify input))
      "kangaroo" "kangaroo"
      "kangaroo" "Kangaroo"
      "penguin-books" "Penguin Books"
      "" "")))

(deftest clean-db-vals-test
  (testing "Test whether nil values are properly cleaned from a map."
    (let [no-change-map {:foo :bar
                         :baz :bam}
          time-map {:bar #?(:clj {:bar (java.util.Date.)}
                            :cljs {:bar (js/Date.)})}]
      (is (= no-change-map (tools/clean-db-vals no-change-map)))
      (is (= 2 (count (tools/clean-db-vals (merge no-change-map {:unwished-for nil})))))
      (is (= {} (tools/clean-db-vals {})))
      (is (= {} (tools/clean-db-vals {:foo ""})))
      (is (= time-map (tools/clean-db-vals time-map))))))