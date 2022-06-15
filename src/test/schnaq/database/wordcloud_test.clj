(ns schnaq.database.wordcloud-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest show-discussion-wordcloud-test
  (testing "Activating the wordcloud."
    (let [share-hash "cat-dog-hash"
          _act (wordcloud-db/show-discussion-wordcloud share-hash)
          visible-wordcloud? (get-in (discussion-db/discussion-by-share-hash share-hash)
                                     [:discussion/wordcloud :wordcloud/visible?])]
      (is visible-wordcloud?))))

(deftest hide-discussion-wordcloud-test
  (testing "Hide the wordcloud."
    (let [share-hash "cat-dog-hash"
          _act (wordcloud-db/hide-discussion-wordcloud share-hash)
          visible-wordcloud? (get-in (discussion-db/discussion-by-share-hash share-hash)
                                     [:discussion/wordcloud :wordcloud/visible?])]
      (is (not visible-wordcloud?)))))
