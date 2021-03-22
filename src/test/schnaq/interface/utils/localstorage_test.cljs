(ns schnaq.interface.utils.localstorage-test
  (:require [cljs.test :refer [deftest are testing]]
            [schnaq.interface.utils.localstorage :as ls]))

(deftest stringify-test
  (testing "Atomic datatypes should produce a valid string."
    (are [x y] (= x y)
               "kangaroo/name" (#'ls/stringify :kangaroo/name)
               "christian" (#'ls/stringify :christian)
               "42" (#'ls/stringify '42)
               "42" (#'ls/stringify "42"))))
