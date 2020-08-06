(ns meetly.toolbelt-test
  (:require [clojure.test :refer [deftest testing is]]
            [meetly.toolbelt :as tools]))

(deftest conforms?-test
  (testing "Checks whether the conforms? sugar works correctly."
    (is (tools/conforms? :meeting/description "A String"))
    (is (not (tools/conforms? :meeting/title 123)))))