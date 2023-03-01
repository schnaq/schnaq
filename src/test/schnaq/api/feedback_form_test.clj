(ns schnaq.api.feedback-form-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [use-fixtures is deftest testing]]
   [muuntaja.core :as m]
   [schnaq.database.main :as db]
   [schnaq.database.patterns :as patterns]
   [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest create-form-test
  (testing "Creating a valid feedback form."
    (let [share-hash "cat-dog-hash"
          request (fn [user-token]
                    (-> {:request-method :post
                         :uri "/discussion/feedback/form"
                         :body-params {:items '({:feedback.item/ordinal 1
                                                 :feedback.item/label "Wie gut war die Vorlesung?"
                                                 :feedback.item/type :feedback.item.type/scale-five}
                                                {:feedback.item/ordinal 3
                                                 :feedback.item/label "Was würdest du mir gerne mitteilen?"
                                                 :feedback.item/type :feedback.item.type/text})
                                       :share-hash share-hash}}
                        toolbelt/add-csrf-header
                        (toolbelt/mock-authorization-header user-token)))]
      (testing "Pro user, that has no moderation rights cannot create items."
        (is (= 403 (-> toolbelt/token-schnaqqifant-user request test-app :status))))
      (testing "Adding items as a moderator is okay"
        (let [response (-> toolbelt/token-n2o-admin request test-app)]
          (is (= 200 (:status response)))
          (is (s/valid? :db/id (:feedback-form-id (m/decode-response-body response)))))))))

(deftest create-form-test-empty-items
  (testing "Creating a valid feedback form  wit no items is not allowed."
    (let [share-hash "cat-dog-hash"
          request (fn [user-token]
                    (-> {:request-method :post
                         :uri "/discussion/feedback/form"
                         :body-params {:items '()
                                       :share-hash share-hash}}
                        toolbelt/add-csrf-header
                        (toolbelt/mock-authorization-header user-token)))]
      (is (= 400 (-> toolbelt/token-n2o-admin request test-app :status))))))

(deftest answer-feedback-test
  (testing "Accept all valid answers without problem."
    (let [share-hash "cat-dog-hash"
          _ (-> {:request-method :post
                 :uri "/discussion/feedback/form"
                 :body-params {:items '({:feedback.item/ordinal 1
                                         :feedback.item/label "Was würdest du mir gerne mitteilen?"
                                         :feedback.item/type :feedback.item.type/text})
                               :share-hash share-hash}}
                toolbelt/add-csrf-header
                (toolbelt/mock-authorization-header toolbelt/token-n2o-admin)
                test-app)
          feedback-id (:discussion/feedback (db/fast-pull [:discussion/share-hash share-hash] patterns/discussion))
          item-id (-> (db/fast-pull feedback-id '[*])
                      :feedback/items
                      first
                      :db/id)
          answer-request (fn [answers]
                           (-> {:request-method :post
                                :uri "/discussion/feedback"
                                :body-params {:answers answers
                                              :share-hash share-hash}}
                               toolbelt/add-csrf-header
                               (toolbelt/mock-authorization-header toolbelt/token-n2o-admin)))
          valid-answer {:feedback.answer/item item-id
                        :feedback.answer/text "Alles!"}]
      (is (= 200 (:status (test-app (answer-request [valid-answer])))))
      (is (:saved? (m/decode-response-body (test-app (answer-request [valid-answer])))))
      (is (= 400 (:status (test-app (answer-request [(assoc valid-answer :feedback.answer/item 123)]))))))))
