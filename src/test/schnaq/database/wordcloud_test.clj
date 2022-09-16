(ns schnaq.database.wordcloud-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
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

(deftest add-word-to-wordcloud-test
  (let [wordcloud-id (:db/id (wordcloud-db/create-local-wordcloud "simple-hash" "Welche Buchstaben kennst du?"))
        get-words #(set (:wordcloud/words (db/fast-pull wordcloud-id patterns/local-wordcloud)))]
    (testing "Adding a new word creates a new tuple with count of 1."
      (wordcloud-db/add-word-to-wordcloud wordcloud-id "Ä")
      (wordcloud-db/add-word-to-wordcloud wordcloud-id "Tuba")
      (is (contains? (get-words) ["Ä" 1]))
      (is (contains? (get-words) ["Tuba" 1])))))

(deftest add-word-to-wordcloud-test-2
  (let [wordcloud-id (:db/id (wordcloud-db/create-local-wordcloud "simple-hash" "Welche Buchstaben kennst du?"))
        get-words #(set (:wordcloud/words (db/fast-pull wordcloud-id patterns/local-wordcloud)))]
    (wordcloud-db/add-word-to-wordcloud wordcloud-id "Ä")
    (dotimes [_n 3] (wordcloud-db/add-word-to-wordcloud wordcloud-id "Tuba"))
    (testing "Repeating a word should increment its count. The old tuple should vanish."
      (is (not (contains? (get-words) ["Tuba" 1])))
      (is (not (contains? (get-words) ["Tuba" 2])))
      (is (contains? (get-words) ["Tuba" 3])))
    (testing "Incrementing one word leaves the others untouched"
      (is (contains? (get-words) ["Ä" 1])))
    (testing "The set cast in this test does not obscure duplicates"
      (is (= 2 (count (:wordcloud/words (db/fast-pull wordcloud-id patterns/local-wordcloud))))))))
