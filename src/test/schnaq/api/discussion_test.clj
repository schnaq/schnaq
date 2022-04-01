(ns schnaq.api.discussion-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest add-label-test
  (let [share-hash "cat-dog-hash"
        statement-id (:db/id (first (discussion-db/starting-statements share-hash)))
        request #(-> {:request-method :put :uri "/discussion/statement/label/add"}
                     (assoc :body-params {:label ":check"
                                          :display-name "A. Schneider"
                                          :share-hash share-hash
                                          :statement-id statement-id})
                     toolbelt/add-csrf-header
                     (toolbelt/mock-authorization-header %))]
    (testing "Only request with valid role shall be accepted."
      @(discussion-db/mods-mark-only! share-hash true)
      (is (= 200 (-> toolbelt/token-schnaqqifant-user request test-app :status)))
      (is (= 403 (-> toolbelt/token-wegi-no-beta-user request test-app :status))))))

;; -----------------------------------------------------------------------------

(defn- get-starting-conclusions-request [user-token share-hash]
  (-> {:request-method :get :uri (:path (api/route-by-name :api.discussion.conclusions/starting))
       :query-params {:share-hash share-hash
                      :display-name "someone"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      m/decode-response-body))

(deftest get-starting-conclusions-test
  (testing "Query starting conclusions."
    (is (not (zero? (count (:starting-conclusions (get-starting-conclusions-request toolbelt/token-wegi-no-beta-user "cat-dog-hash"))))))
    (is (not (zero? (count (:starting-conclusions (get-starting-conclusions-request nil "cat-dog-hash"))))))
    (is (string? (:message (get-starting-conclusions-request nil ":shrug:"))))))

(defn- react-to-conclusions-request [statement-id]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.discussion.react-to/statement))
       :body-params {:share-hash "cat-dog-hash"
                     :conclusion-id statement-id
                     :premise "test"
                     :statement-type :statement.type/attack
                     :locked? false
                     :display-name "Wugiman"}}
      toolbelt/add-csrf-header
      test-app))

(deftest react-to-any-statement!-test
  (let [starting-statements (discussion-db/starting-statements "cat-dog-hash")
        locked-statement (first (filter :statement/locked? starting-statements))
        unlocked-statement (first (remove :statement/locked? starting-statements))]
    (is (= 201 (:status (react-to-conclusions-request (:db/id unlocked-statement)))))
    (is (= 403 (:status (react-to-conclusions-request (:db/id locked-statement)))))))
