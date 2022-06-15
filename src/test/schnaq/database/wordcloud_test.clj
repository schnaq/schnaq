(ns schnaq.database.wordcloud-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest show-discussion-wordcloud-test
  (testing "Activating the wordcloud."
    (let [share-hash "cat-dog-hash"
          _act (wordcloud-db/show-discussion-wordcloud share-hash true)
          {:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (is (= true visible?)))))

(deftest hide-discussion-wordcloud-test
  (testing "Hide the wordcloud."
    (let [share-hash "cat-dog-hash"
          _act (wordcloud-db/show-discussion-wordcloud share-hash false)
          {:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (is (= false visible?)))))

(deftest wordcloud-by-share-hash-test
  (testing "Return the wordcloud."
    (let [share-hash "cat-dog-hash"
          _act (wordcloud-db/show-discussion-wordcloud share-hash true)
          {:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (is (= true visible?)))))
