(ns schnaq.interface.views.meeting.calendar-invite-test
  (:require [cljs.test :refer [deftest testing is are]]
            [cljs-time.core :as time]
            [schnaq.interface.views.meeting.calendar-invite :refer [parse-datetime]]))

(deftest parse-datetime-test
  (testing "Parsing valid strings results in valid data, surprise!"
    (is (time/equal? (time/date-time 2020 10 15 13 37)
                     (parse-datetime "2020/10/15 13:37")))))

