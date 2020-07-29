(ns meetly.meeting.database-test
  (:require [clojure.test :refer [deftest testing]]))

;; Preparation when testing with database is possible.
;;
;; See dev-local setup of dialog.core and the testing-toolbelt functions for
;; testing fixtures -- everything is already set up there. We just need to copy
;; it from there

(deftest agenda-by-meeting-hash-and-discussion-id-test
  (testing "If discussion/agenda-id belongs to a meeting, it should return the agenda."))
;; (db/agenda-by-meeting-hash-and-discussion-id "2d2d3a83-2296-4b81-b544-1e7c4a607cdd" 17592186045433)))