(ns schnaq.api.feedback-form-test
  (:require
   [clojure.test :refer [use-fixtures is deftest testing]]
   [muuntaja.core :as m]
   [schnaq.database.main :as db]
   [schnaq.database.patterns :as patterns]
   [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(defn- create-test-form
  "Creates a quick form in the Cat or Dog discussion."
  ([user-token]
   (create-test-form user-token '({:feedback.item/ordinal 1
                                   :feedback.item/label "Wie gut war die Vorlesung?"
                                   :feedback.item/type :feedback.item.type/scale-five}
                                  {:feedback.item/ordinal 3
                                   :feedback.item/label "Was würdest du mir gerne mitteilen?"
                                   :feedback.item/type :feedback.item.type/text})))
  ([user-token items]
   (-> {:request-method :post
        :uri "/discussion/feedback/form"
        :body-params {:items items
                      :share-hash "cat-dog-hash"}}
       toolbelt/add-csrf-header
       (toolbelt/mock-authorization-header user-token))))

(deftest create-form-test
  (testing "Creating a valid feedback form."
    (testing "Pro user, that has no moderation rights cannot create items."
      (is (= 403 (-> toolbelt/token-schnaqqifant-user create-test-form test-app :status))))
    (testing "Adding items as a moderator is okay"
      (let [response (-> toolbelt/token-n2o-admin create-test-form test-app)]
        (is (= 200 (:status response)))
        ;; Returns an id
        (is (pos-int? (:feedback-form-id (m/decode-response-body response))))))))

(deftest create-form-test-empty-items
  (testing "Creating a valid feedback form  wit no items is not allowed."
    (is (= 400 (-> toolbelt/token-n2o-admin (create-test-form '()) test-app :status)))))

(defn- answer-feedback
  [answers]
  (-> {:request-method :post
       :uri "/discussion/feedback"
       :body-params {:answers answers
                     :share-hash "cat-dog-hash"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header toolbelt/token-n2o-admin)))

(deftest answer-feedback-test
  (testing "Accept all valid answers without problem."
    (let [share-hash "cat-dog-hash"
          _ (create-test-form toolbelt/token-n2o-admin
                              '({:feedback.item/ordinal 1
                                 :feedback.item/label "Was würdest du mir gerne mitteilen?"
                                 :feedback.item/type :feedback.item.type/text}))
          feedback-id (:discussion/feedback (db/fast-pull [:discussion/share-hash share-hash] patterns/discussion))
          item-id (-> (db/fast-pull feedback-id '[*])
                      :feedback/items
                      first
                      :db/id)
          valid-answer {:feedback.answer/item item-id
                        :feedback.answer/text "Alles!"}]
      (is (= 200 (:status (test-app (answer-feedback [valid-answer])))))
      (is (:saved? (m/decode-response-body (test-app (answer-feedback [valid-answer])))))
      (is (= 400 (:status (test-app (answer-feedback [(assoc valid-answer :feedback.answer/item 123)]))))))))
