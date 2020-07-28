(ns meetly.interface.views.discussion-test
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [meetly.meeting.interface.views.discussion :as d]))

(deftest test-nothing
  (is (= 1 1)))