(ns meetly.meeting.dialog-connector
  (:import (java.util UUID)))

;; TODO this needs to be wired in, when the dialog.core is done.
(defn create-discussion-for-agenda
  "Creates a discussion for an agenda-point and returns some identifier for the agenda
  to save."
  [_title _description]
  (str (UUID/randomUUID)))

(defn starting-conclusions
  "Return the starting conclusions for a given `discussion-id`."
  [_discussion-id]
  ;; TODO Do a query to the engine to start-discussion.
  ;; then take only the conclusions of the answer.
  [{:db/id 17592186045431,
    :statement/content "we could get both, a dog and a cat",
    :statement/version 1,
    :statement/author #:author{:nickname "Christian"}}
   {:db/id 17592186045429,
    :statement/content "we should get a dog",
    :statement/version 1,
    :statement/author #:author{:nickname "Wegi"}}
   {:db/id 17592186045430,
    :statement/content "we should get a cat",
    :statement/version 1,
    :statement/author #:author{:nickname "Der Schredder"}}])