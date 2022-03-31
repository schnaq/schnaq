(ns schnaq.shared-toolbelt-test
  (:require [clojure.test :refer [deftest is are testing]]
            [schnaq.shared-toolbelt :as tools]
            [schnaq.test-data :refer [kangaroo christian]]))

(deftest slugify-test
  (testing "Slugs should contain no whitespace or other special characters."
    (are [slugged input] (= slugged (tools/slugify input))
      "kangaroo" "kangaroo"
      "kangaroo" "Kangaroo"
      "penguin-books" "Penguin Books"
      "kangaroo" "/kangaroo"
      "anti-social-network" "/anti-social.network"
      "" "")))

(deftest clean-db-vals-test
  (testing "Test whether nil values are properly cleaned from a map."
    (let [no-change-map {:foo :bar
                         :baz :bam}
          time-map {:bar #?(:clj {:bar (java.util.Date.)}
                            :cljs {:bar (js/Date.)})}]
      (is (= no-change-map (tools/remove-nil-values-from-map no-change-map)))
      (is (= 2 (count (tools/remove-nil-values-from-map (merge no-change-map {:unwished-for nil})))))
      (is (= {} (tools/remove-nil-values-from-map {})))
      (is (= {} (tools/remove-nil-values-from-map {:foo ""})))
      (is (= time-map (tools/remove-nil-values-from-map time-map))))))

(deftest normalize-test
  (testing "Normalize data for better access through a map."
    (let [normalized (tools/normalize :db/id [kangaroo christian])]
      (is (= christian (get normalized (:db/id christian))))
      (is (= kangaroo (get normalized (:db/id kangaroo))))
      (is (nil? (get normalized "razupaltuff"))))))
