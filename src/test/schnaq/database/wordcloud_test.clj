(ns schnaq.database.wordcloud-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.wordcloud :as wordcloud-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest toggle-wordcloud-visibility-test
  (testing "Activating the wordcloud."
    (let [share-hash "simple-hash"
          _act (wordcloud-db/toggle-wordcloud-visibility share-hash)
          {:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (is (= true visible?)))))

(deftest double-toggle-wordcloud-visibility-test
  (testing "Hide the wordcloud."
    (let [share-hash "cat-dog-hash"
          _disable (wordcloud-db/toggle-wordcloud-visibility share-hash)
          {:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash share-hash)]
      (is (= false visible?)))))

(deftest wordcloud-by-share-hash-test
  (testing "Return the wordcloud."
    (let [{:keys [wordcloud/visible?]} (wordcloud-db/wordcloud-by-share-hash "cat-dog-hash")]
      (is (= true visible?)))))

(deftest wordcloud-by-share-hash-non-existent-discussion-test
  (testing "Return nil if the discussion does not exist."
    (is (nil? (wordcloud-db/wordcloud-by-share-hash "not-a-real-share-hash")))))

(deftest wordcloud-by-share-hash-no-wordcloud-test
  (testing "Return nil if there is no wordcloud in the discussion."
    (is (nil? (wordcloud-db/wordcloud-by-share-hash "simple-hash")))))
