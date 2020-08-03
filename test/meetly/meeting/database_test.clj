(ns meetly.meeting.database-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [dialog.test.toolbelt :as meetly-toolbelt]
            [dialog.discussion.database :as ddb]
            [meetly.meeting.database :as database]))


(use-fixtures :each meetly-toolbelt/init-db-test-fixture)


#_(deftest agenda-by-meeting-hash-and-discussion-id-test
    (testing "If discussion/agenda-id belongs to a meeting, it should return the agenda."))
;; (db/agenda-by-meeting-hash-and-discussion-id "2d2d3a83-2296-4b81-b544-1e7c4a607cdd" 17592186045433)))

(deftest up-and-downvotes-test
  (testing "Tests whether setting up and downvotes works properly."
    (let [cat-or-dog (:db/id (ddb/all-discussions-by-title "Cat or Dog?"))
          some-statements (map #(get-in % [:argument/premises first :db/id])
                               (ddb/all-arguments-for-discussion cat-or-dog))
          author-1 "Test-1"
          author-2 "Test-2"]
      (database/add-author-if-not-exists author-1)
      (database/add-author-if-not-exists author-2)
      (database/upvote-statement! (first some-statements) author-1)
      (database/downvote-statement! (second some-statements) author-1)
      (database/upvote-statement! (first some-statements) author-2)
      (is (= 2 (database/upvotes-for-statement (first some-statements))))
      (is (= 1 (database/downvotes-for-statement (second some-statements))))
      (is (= 0 (database/downvotes-for-statement (first some-statements)))))))