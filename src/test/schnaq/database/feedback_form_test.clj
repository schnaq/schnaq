(ns schnaq.database.feedback-form-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.feedback-form
             :refer [new-feedback-form! update-feedback-form-items! delete-feedback! feedback-items add-answers]]
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
      (is (= "What do you want to tell me?"
             (->> feedback-items
                  (filter #(= 1 (:feedback.item/ordinal %)))
                  first
                  :feedback.item/label))))))

(deftest update-feedback-form-items!-empty-feedback-test
  (testing "Updating items for discussion without feedback does nothing"
    (let [share-hash "cat-dog-hash"
          result (update-feedback-form-items! share-hash
                                              [{:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "bla"
                                                :feedback.item/ordinal 1}]
                                              false)]
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
          result (update-feedback-form-items! share-hash [] false)]
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
          result (update-feedback-form-items! share-hash
                                              [{:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "blubb"
                                                :feedback.item/ordinal 1}
                                               {:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "foo"
                                                :feedback.item/ordinal 2}]
                                              true)
          updated-feedback (fast-pull feedback-id '[*])]
      (is (not (nil? result)))
      (is 2 (count (:feedback/items updated-feedback)))
      (is "blubber" (->> (:feedback/items updated-feedback)
                         (filter #(= 1 (:feedback.item/ordinal %)))
                         first
                         :feedback.item/label))
      (is (:feedback/visible updated-feedback)))))

(deftest update-feedback-form-items!-without-id-is-double-test
  (testing "Updating items with id edits, without adds item"
    (let [share-hash "cat-dog-hash"
          first-item-id (->> @(transact [{:db/id "new-feedback"
                                          :feedback/items {:feedback.item/type :feedback.item.type/text
                                                           :feedback.item/label "bla"
                                                           :feedback.item/ordinal 1
                                                           :db/id "first-item"}}
                                         [:db/add [:discussion/share-hash share-hash]
                                          :discussion/feedback "new-feedback"]])
                             :tempids
                             (#(% "first-item")))
          _ (println first-item-id)
          feedback-id (:discussion/feedback (fast-pull [:discussion/share-hash share-hash] patterns/discussion))
          result (update-feedback-form-items! share-hash
                                              [{:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "blubb"
                                                :feedback.item/ordinal 1}
                                               {:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "foo"
                                                :feedback.item/ordinal 2}
                                               {:feedback.item/type :feedback.item.type/text
                                                :feedback.item/label "blabla"
                                                :feedback.item/ordinal 4
                                                :db/id first-item-id}]
                                              true)
          updated-feedback (fast-pull feedback-id '[*])]
      (is (not (nil? result)))
      (is 3 (count (:feedback/items updated-feedback)))
      (is "blubber" (->> (:feedback/items updated-feedback)
                         (filter #(= 1 (:feedback.item/ordinal %)))
                         first
                         :feedback.item/label))
      (is (:feedback/visible updated-feedback)))))

(deftest delete-feedback!-test
  (testing "Deleting feedback works easy."
    (let [share-hash "cat-dog-hash"
          _ @(transact [{:db/id "new-feedback"
                         :feedback/items {:feedback.item/type :feedback.item.type/text
                                          :feedback.item/label "bla"
                                          :feedback.item/ordinal 1}}
                        [:db/add [:discussion/share-hash share-hash]
                         :discussion/feedback "new-feedback"]])]
      (delete-feedback! share-hash)
      (is (nil? (:discussion/feedback (fast-pull [:discussion/share-hash share-hash] patterns/discussion))))
      (testing "Discussion without feedback is not modified"
        (is (nil? (delete-feedback! "simple-hash")))))))

(deftest feedback-form-test
  (testing "Retrieve feedback-items"
    (let [share-hash "cat-dog-hash"
          _ (transact [{:db/id "new-feedback"
                        :feedback/items {:feedback.item/type :feedback.item.type/text
                                         :feedback.item/label "bla"
                                         :feedback.item/ordinal 1}}
                       [:db/add [:discussion/share-hash share-hash]
                        :discussion/feedback "new-feedback"]])
          _ (update-feedback-form-items! share-hash
                                         [{:feedback.item/type :feedback.item.type/text
                                           :feedback.item/label "blubb"
                                           :feedback.item/ordinal 1}
                                          {:feedback.item/type :feedback.item.type/text
                                           :feedback.item/label "foo"
                                           :feedback.item/ordinal 2}]
                                         true)
          retrieved-items (feedback-items share-hash)]
      (is (seq retrieved-items))
      (is 2 (count retrieved-items))
      (is "blubber" (->> retrieved-items
                         (filter #(= 1 (:feedback.item/ordinal %)))
                         first
                         :feedback.item/label)))))

(deftest add-answers-test
  (testing "Add answers to questions works as expected."
    (let [share-hash "cat-dog-hash"
          _ (transact [{:db/id "new-feedback"
                        :feedback/items {:feedback.item/type :feedback.item.type/text
                                         :feedback.item/label "bla"
                                         :feedback.item/ordinal 1}}
                       [:db/add [:discussion/share-hash share-hash]
                        :discussion/feedback "new-feedback"]])
          feedback-id (:discussion/feedback (fast-pull [:discussion/share-hash share-hash] patterns/discussion))
          item-id (-> (fast-pull feedback-id '[*])
                      :feedback/items
                      first
                      :db/id)
          answer-result (add-answers share-hash [{:feedback.answer/item item-id
                                                  :feedback.answer/text "Foobar teach!"}])
          first-answer (-> (fast-pull feedback-id '[*])
                           :feedback/answers
                           first
                           :db/id
                           (fast-pull '[*]))]
      (is (not (add-answers "simple-hash" {})))
      (is (not (add-answers share-hash {:some-other :key})))
      (is answer-result)
      (is (= "Foobar teach!" (:feedback.answer/text first-answer))))))
