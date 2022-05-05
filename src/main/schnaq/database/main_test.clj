(ns schnaq.database.main-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [set-activation-focus]]
            [schnaq.database.poll :as poll-db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest set-activation-focus-empty-test
  (testing "If no activation is set as focus, no id is stored in discussion."
    (let [discussion (discussion-db/discussion-by-share-hash "cat-dog-hash")]
      (is (nil? (:discussion/activation-focus discussion))))))

(deftest set-activation-focus-test
  (testing "Set a poll as a focus and its id is stored in the discussion."
    (let [share-hash "cat-dog-hash"
          poll-id (:db/id (first (poll-db/polls share-hash)))
          _ (set-activation-focus [:discussion/share-hash share-hash] poll-id)
          discussion (discussion-db/discussion-by-share-hash share-hash)]
      (is (= poll-id
             (:discussion/activation-focus discussion))))))
