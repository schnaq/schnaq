(ns schnaq.interface.views.meeting.calendar-invite-test
  (:require [cljs.test :refer [deftest testing is are]]
            [schnaq.interface.views.meeting.calendar-invite :refer [parse-date]]))

(deftest parse-date-test
  (testing "Parsing valid strings results in valid data, surprise!"
    (is (= {:year 2020, :month 10, :day 15, :hour 13, :minute 37}
           (parse-date "2020/10/15 13:37")))))
