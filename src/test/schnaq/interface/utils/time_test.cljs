(ns schnaq.interface.utils.time-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [schnaq.interface.utils.time :as util-time]))

(deftest format-distance-test
  (testing "Relative times are written in the requested language."
    (is (str/starts-with? (util-time/format-distance (js/Date.) :de) "vor"))
    (is (str/ends-with? (util-time/format-distance (js/Date.) :en) "ago"))))
