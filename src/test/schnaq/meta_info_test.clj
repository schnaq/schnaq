(ns schnaq.meta-info-test
  (:require [clojure.test :refer [deftest testing use-fixtures is]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.meta-info :as meta-info]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest meta-infos-test
  (testing "Tests if number of posts are correct and authors increase after adding a new one to the discussion\n"
    (let [share-hash "simple-hash"
          author {:user/nickname "Wegi"}
          all-statements (discussion-db/all-statements share-hash)
          total-count (count all-statements)
          meta-infos (#'meta-info/discussion-meta-info share-hash author)
          ;; add starting argument
          statement "Clojure can melt steelbeams"
          user-id (user-db/add-user "New Person")
          _ (discussion-db/add-starting-statement! share-hash user-id statement)
          ;; new meta infos
          new-meta-infos (#'meta-info/discussion-meta-info share-hash author)]
      (testing "Test if total count is correct"
        (is (= total-count (:all-statements meta-infos))))
      (testing "Test if total count is increased after adding a new statement"
        (is (= (inc total-count) (:all-statements new-meta-infos))))
      (testing "Test if author count is increased after adding a new statement by a new user"
        (is (= (inc (count (:authors meta-infos)))
               (count (:authors new-meta-infos))))))))
