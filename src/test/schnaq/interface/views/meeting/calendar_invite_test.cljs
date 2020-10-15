(ns schnaq.interface.views.meeting.calendar-invite-test
  (:require [cljs.test :refer [deftest testing is are]]
            [schnaq.interface.views.meeting.calendar-invite :refer [parse-date]]))

(def sample "2020/10/15 13:37")

(deftest parse-date-test

  (is (= 1 0)))
