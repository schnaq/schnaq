(ns schnaq.database.visible-entities-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.database.visible-entity :as visible-entities-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def test-share-hash "cat-dog-hash")
(def test-share-hash-visible-entities "share-hash-visible-entities")

(deftest add-entity-test
  (testing "Add a visible entity to a discussion."
    (visible-entities-db/add-entity! test-share-hash
                                     :discussion.visible.entities/wordcloud)
    (is (true? (some #(= % :discussion.visible.entities/wordcloud)
                     (visible-entities-db/get-entities test-share-hash))))))

(deftest retract-entity-test
  (testing "Retract a visible entity from a discussion."
    (visible-entities-db/retract-entity! test-share-hash-visible-entities
                                         :discussion.visible.entities/wordcloud)
    (is (nil? (some #(= % :discussion.visible.entities/wordcloud)
                    (visible-entities-db/get-entities test-share-hash-visible-entities))))))