(ns meetly.meeting.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [dialog.discussion.database :as ddb]
            [meetly.meeting.processors :as processors]))

(use-fixtures :each meetly-toolbelt/init-db-test-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [cat-or-dog (:db/id (first (ddb/all-discussions-by-title "Cat or Dog?")))
          enriched-data (processors/with-votes (ddb/starting-arguments-by-discussion cat-or-dog))
          statements-only (filter #(contains? % :statement/version) enriched-data)
          upvotes-only (filter number? (map :meta/upvotes statements-only))
          downvotes-only (filter number? (map :meta/downvotes statements-only))]
      (is (= (count statements-only) (count upvotes-only) (count downvotes-only))))))