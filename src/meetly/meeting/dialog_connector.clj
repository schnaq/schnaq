(ns meetly.meeting.dialog-connector
  (:import (java.util UUID)))

;; TODO this needs to be wired in, when the dialog.core is done.
(defn create-discussion-for-agenda
  "Creates a discussion for an agenda-point and returns some identifier for the agenda
  to save."
  [_title _description]
  (str (UUID/randomUUID)))