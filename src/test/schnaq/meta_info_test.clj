(ns schnaq.meta-info-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as main-db]
            [schnaq.meeting.meta-info :as meta-info]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest meta-infos-test
  (testing "Tests if number of posts are correct and authors increase after adding a new one to the discussion\n"
    (let [share-hash "simple-hash"
          discussion (discussion-db/discussion-by-share-hash share-hash)
          starting (:discussion/starting-statements discussion)
          arguments (discussion-db/all-arguments-for-discussion share-hash)
          total-count (+ (count starting) (count arguments))
          meta-infos (#'meta-info/discussion-meta-info share-hash)
          ;; add starting argument
          statement "Clojure can melt steelbeams"
          user-id (main-db/add-user "New Person")
          _ (discussion-db/add-starting-statement! share-hash user-id statement)
          ;; new meta infos
          new-meta-infos (#'meta-info/discussion-meta-info share-hash)]
      (testing "Test if total count is correct"
        (is (= total-count (:all-statements meta-infos))))
      (testing "Test if total count is increased after adding a new statement"
        (is (= (inc total-count) (:all-statements new-meta-infos))))
      (testing "Test if author count is increased after adding a new statement by a new user"
        (is (= (inc (count (:authors meta-infos)))
               (count (:authors new-meta-infos))))))))
