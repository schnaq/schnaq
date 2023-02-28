(ns schnaq.database.feedback-form-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.feedback-form :refer [new-feedback-form! update-feedback-form-items!]]
            [schnaq.database.main :refer [fast-pull transact]]
            [schnaq.database.patterns :as patterns]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest new-feedback-form!-empty-items-test
  (testing "Transact a new form without items. Should return nil and not transact."
    (let [share-hash "cat-dog-hash"
          result (new-feedback-form! share-hash '())]
      (is (nil? result))
      (is (nil?
           (:discussion/feedback
            (fast-pull [:discussion/share-hash share-hash] [{:discussion/feedback patterns/feedback-form}])))))))

(deftest new-feedback-form!-items-test
  (testing "Transact a new form with two items. Should return them."
    (let [share-hash "cat-dog-hash"
          result (new-feedback-form! share-hash '({:feedback.item/type :feedback.item.type/text
                                                   :feedback.item/label "What do you want to tell me?"
                                                   :feedback.item/ordinal 1}
                                                  {:feedback.item/type :feedback.item.type/scale-five
                                                   :feedback.item/label "How good is the lecture? (5 being best)"
                                                   :feedback.item/ordinal 2}))
          feedback-items (get-in
                          (fast-pull [:discussion/share-hash share-hash] [{:discussion/feedback patterns/feedback-form}])
                          [:discussion/feedback :feedback/items])]
      (is (pos-int? result))
      (is (= 2 (count feedback-items)))
      (is (some #{{:feedback.item/type :feedback.item.type/text
                   :feedback.item/label "What do you want to tell me?"
                   :feedback.item/ordinal 1}}
                feedback-items)))))

(deftest update-feedback-form-items!-empty-feedback-test
  (testing "Updating items for discussion without feedback does nothing"
    (let [share-hash "cat-dog-hash"
          result (update-feedback-form-items! share-hash [{:feedback.item/type :feedback.item.type/text
                                                           :feedback.item/label "bla"
                                                           :feedback.item/ordinal 1}])]
      (is (nil? result)))))

(deftest update-feedback-form-items!-empty-items-test
  (testing "Updating empty items for feedback does nothing"
    (let [share-hash "cat-dog-hash"
          _ (transact [{:db/id "new-feedback"
                        :feedback/items {:feedback.item/type :feedback.item.type/text
                                         :feedback.item/label "bla"
                                         :feedback.item/ordinal 1}}
                       [:db/add [:discussion/share-hash share-hash]
                        :discussion/feedback "new-feedback"]])
          result (update-feedback-form-items! share-hash [])]
      (is (nil? result)))))


(deftest update-feedback-form-items!-test
  (testing "Updating items works as expected"
    (let [share-hash "cat-dog-hash"
          _ (transact [{:db/id "new-feedback"
                        :feedback/items {:feedback.item/type :feedback.item.type/text
                                         :feedback.item/label "bla"
                                         :feedback.item/ordinal 1}}
                       [:db/add [:discussion/share-hash share-hash]
                        :discussion/feedback "new-feedback"]])
          feedback-id (:discussion/feedback (fast-pull [:discussion/share-hash share-hash] patterns/discussion))
          result (update-feedback-form-items! share-hash [{:feedback.item/type :feedback.item.type/text
                                                           :feedback.item/label "blubb"
                                                           :feedback.item/ordinal 1}
                                                          {:feedback.item/type :feedback.item.type/text
                                                           :feedback.item/label "foo"
                                                           :feedback.item/ordinal 2}])
          updated-feedback (fast-pull feedback-id '[*])]
      (is (not (nil? result)))
      (is 2 (count (:feedback/items updated-feedback)))
      (is "blubber" (->> (:feedback/items updated-feedback)
                         (filter #(= 1 (:feedback.item/ordinal %)))
                         first
                         :feedback.item/label)))))
