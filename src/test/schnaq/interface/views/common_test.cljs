(ns schnaq.interface.views.common-test
  (:require [clojure.test :refer [deftest testing are]]
            [schnaq.interface.views.common :refer [add-namespace-to-keyword]]))

(deftest add-namespace-to-keyword-test
  (testing "Prepend namespace to keyword."
    (are [result to-prepend to-keyword]
      (= result (add-namespace-to-keyword to-prepend to-keyword))
      :foo/bar :foo :bar
      :namespace/test "namespace" :test
      :namespace/test :namespace "test"
      :all/strings "all" "strings")))

