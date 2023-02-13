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

(deftest namespaced-keyword->string-test
  (are [x y] (= y (tools/namespaced-keyword->string x))
    nil nil
    :foo "foo"
    :foo/bar "foo/bar"
    :foo.bar/baz "foo.bar/baz"))

(deftest pro-user?-test
  (testing "Valid pro user roles are true."
    (are [roles result] (= result (tools/pro-user? roles))
      #{} false
      #{:foo} false
      #{:foo :bar} false
      #{:role/pro} true
      #{:role/pro :role/foo} true
      #{:role/enterprise} true
      #{:role/tester} true
      #{:role/admin} true
      #{:role/analytics} true
      #{:role/admin :role/pro} true)))

(deftest beta-tester?-test
  (testing "Check valid beta-tester roles"
    (are [roles result] (= result (tools/beta-tester? roles))
      #{} false
      #{:foo} false
      #{:foo :bar} false
      #{:role/pro} false
      #{:role/pro :role/foo} false
      #{:role/enterprise} false
      #{:role/admin} true
      #{:role/admin :role/pro} true
      #{:role/analytics} true
      #{:role/tester} true)))

(deftest admin?-test
  (testing "Check valid admin roles"
    (are [roles result] (= result (tools/admin? roles))
      #{} false
      #{:foo} false
      #{:foo :bar} false
      #{:role/pro} false
      #{:role/pro :role/foo} false
      #{:role/enterprise} false
      #{:role/admin} true
      #{:role/admin :role/pro} true
      #{:role/analytics} false
      #{:role/tester} false)))

(deftest analytics-admin?-test
  (testing "Check valid analytics-admin roles"
    (are [roles result] (= result (tools/analytics-admin? roles))
      #{} false
      #{:foo} false
      #{:foo :bar} false
      #{:role/pro} false
      #{:role/pro :role/foo} false
      #{:role/enterprise} false
      #{:role/admin} true
      #{:role/admin :role/pro} true
      #{:role/analytics} true
      #{:role/tester} false)))

(deftest deep-merge-with-test
  (are [f m1 m2 result] (= result (tools/deep-merge-with f m1 m2))
    + {:a 1} {:b 2} (merge-with + {:a 1} {:b 2})
    + {:a 1} {:a 2} (merge-with + {:a 1} {:a 2})
    merge {:a {:b 1}} {:a {:c 2}} {:a {:b 1 :c 2}}
    + {:a {:b 1}} {:a {:b 2}} {:a {:b 3}}

    + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}}
    {:a {:b {:z 3 :c 3 :d {:z 9 :x 1 :y 2}} :e 103} :f 4}))
