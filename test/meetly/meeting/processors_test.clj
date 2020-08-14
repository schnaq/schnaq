(ns meetly.meeting.processors-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [dialog.discussion.database :as ddb]
            [meetly.meeting.processors :as processors]))

(use-fixtures :each meetly-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once meetly-toolbelt/clean-database-fixture)

(deftest with-votes-processor-test
  (testing "Result should have all statements enriched with votes-metadata"
    (let [cat-or-dog (:db/id (first (ddb/all-discussions-by-title "Cat or Dog?")))
          enriched-data (processors/with-votes (ddb/starting-arguments-by-discussion cat-or-dog))
          statements-only (filter #(contains? % :statement/version) enriched-data)
          upvotes-only (filter number? (map :meta/upvotes statements-only))
          downvotes-only (filter number? (map :meta/downvotes statements-only))]
      (is (= (count statements-only) (count upvotes-only) (count downvotes-only))))))

(deftest with-canonical-usernames-test
  (testing "Tests whether arguments are correctly enriched."
    (is (= [:starting-argument/new
            {:discussion/id 87960930222185, :user/nickname "Wegi"}]
           (processors/with-canonical-usernames
             [:starting-argument/new
              {:discussion/id 87960930222185, :user/nickname "WeGi"}])
           (processors/with-canonical-usernames
             [:starting-argument/new
              {:discussion/id 87960930222185, :user/nickname "WEGI"}])))))