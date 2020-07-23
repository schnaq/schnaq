(ns meetly.interface.views.discussion-test
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [meetly.meeting.interface.views.discussion :as d]))

(deftest test-whatever
  (is (= (d/select-premises [{:argument/conclusion {:db/id 123}
                              :argument/premises [:some-premise]
                              :argument/type :argument.type/support}
                             {:argument/conclusion {:db/id 123}
                              :argument/premises [:attacking-premise :other-premise]
                              :argument/type :argument.type/attack}] 123)
         '((:some-premise :argument.type/support)
           (:attacking-premise :argument.type/attack)
           (:other-premise :argument.type/attack))))
  (is (= (d/select-premises [] 123)
         '())))