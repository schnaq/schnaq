(ns schnaq.interface.utils.localstorage-test
  (:require [cljs.test :refer [deftest is are testing]]
            [schnaq.interface.utils.localstorage :as ls]))

(deftest stringify-test
  (testing "Atomic datatypes should produce a valid string."
    (are [x y] (= x y)
               "kangaroo/name" (#'ls/stringify :kangaroo/name)
               "christian" (#'ls/stringify :christian)
               "42" (#'ls/stringify '42)
               "42" (#'ls/stringify "42"))))

(deftest parse-set-test-simple
  (testing "Test basic functionality of parsing sets to strings and strings to sets."
    (let [string-set-1 #{":23838" ":123" ":1"}
          set-from-string-1 (ls/parse-string-as-set ":1 :1 :123 :23838")
          string-set-2 #{":cars" ":wham" ":acdc"}
          set-from-string-2 (ls/parse-string-as-set ":cars :wham :acdc")
          set-string-1 ":23838"
          string-from-set-1 (#'ls/parse-set-as-string #{":23838"})
          set-string-2 ":wham"
          string-from-set-2 (#'ls/parse-set-as-string #{":wham"})]
      (is (= string-set-1 set-from-string-1))
      (is (= string-set-2 set-from-string-2))
      (is (= set-string-1 string-from-set-1))
      (is (= set-string-2 string-from-set-2)))))

(deftest parse-set-test-back-and-forth
  (testing "Parse sets to strings and back to sets."
    (let [set-original #{":23838" ":123" ":1" "abba"}
          string-from-set (#'ls/parse-set-as-string set-original)
          set-copy (ls/parse-string-as-set string-from-set)]
      (is (= set-original set-copy)))))