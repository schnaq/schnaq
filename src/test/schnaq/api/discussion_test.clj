(ns schnaq.api.discussion-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.poll :as poll-db]
            [schnaq.database.user :as user-db]
            [schnaq.test.toolbelt :as toolbelt :refer [file image test-app]]
            [taoensso.tufte :refer [p profiled]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

;; -----------------------------------------------------------------------------

(defn- statement-label-request [verb share-hash statement-id token]
  (-> {:request-method verb :uri (:path (api/route-by-name :api.discussion.statement/label))}
      (assoc :body-params {:label ":check"
                           :display-name "A. Schneider"
                           :share-hash share-hash
                           :statement-id statement-id})
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token)
      test-app
      :status))

(deftest add-label-no-restriction-test
  (let [share-hash "cat-dog-hash"
        statement-id (:db/id (first (discussion-db/starting-statements share-hash)))
        request-fn (partial statement-label-request :put share-hash statement-id)]
    (testing "Anonymous and registered users are allowed to add labels."
      (is (= 200 (request-fn nil)))
      (is (= 200 (request-fn toolbelt/token-schnaqqifant-user)))
      (is (= 200 (request-fn toolbelt/token-kangaroo-normal-user)))
      (is (= 200 (request-fn toolbelt/token-n2o-admin))))))

(deftest add-label-mods-only-test
  (let [share-hash "cat-dog-hash"
        statement-id (:db/id (first (discussion-db/starting-statements share-hash)))
        request-fn (partial statement-label-request :put share-hash statement-id)
        _ @(discussion-db/mods-mark-only! share-hash true)]
    (testing "Only moderators are allowed to set labels."
      (is (= 403 (request-fn nil)))
      (is (= 403 (request-fn toolbelt/token-kangaroo-normal-user)))
      (is (= 200 (request-fn toolbelt/token-schnaqqifant-user))))))

(deftest remove-label-no-restriction-test
  (let [share-hash "simple-hash"
        statement-id (:db/id (first (discussion-db/statements-by-content "Brainstorming ist total wichtig")))
        request-fn (partial statement-label-request :delete share-hash statement-id)]
    (testing "Anonymous and registered users are allowed to remove labels."
      (is (= 200 (request-fn nil)))
      (is (= 200 (request-fn toolbelt/token-schnaqqifant-user)))
      (is (= 200 (request-fn toolbelt/token-kangaroo-normal-user)))
      (is (= 200 (request-fn toolbelt/token-n2o-admin))))))

(deftest remove-label-mods-only-test
  (let [share-hash "simple-hash"
        statement-id (:db/id (first (discussion-db/statements-by-content "Brainstorming ist total wichtig")))
        request-fn (partial statement-label-request :delete share-hash statement-id)
        _ @(discussion-db/mods-mark-only! share-hash true)]
    (testing "Only moderators are allowed to remove labels."
      (is (= 403 (request-fn nil)))
      (is (= 403 (request-fn toolbelt/token-kangaroo-normal-user)))
      (is (= 200 (request-fn toolbelt/token-schnaqqifant-user))))))

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

(deftest get-starting-conclusions-timing-test
  (testing "Check that the querying of many starting statements is performant"
    (dotimes [_ 500]
      (discussion-db/add-starting-statement!
       "cat-dog-hash" (user-db/user-id "wegi" nil)
       "Foo statements!"))
    (let [stats @(second
                  (profiled
                   {}
                   (dotimes [_ 10]
                     (p :starting-conclusion-api
                        (get-starting-conclusions-request
                         toolbelt/token-wegi-no-beta-user
                         "cat-dog-hash")))))]
      ;; 500 ms
      (is (< (get-in stats [:stats :starting-conclusion-api :mean]) 500000000)))))

(defn- react-to-conclusions-request [statement-id]
  (-> {:request-method :post :uri (:path (api/route-by-name :api.discussion.react-to/statement))
       :body-params {:share-hash "cat-dog-hash"
                     :edit-hash "cat-dog-edit-hash"
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

(deftest toggle-pinned-statement-test
  (let [statement-id (:db/id (first (discussion-db/starting-statements "cat-dog-hash")))
        request #(-> {:request-method :post :uri (:path (api/route-by-name :api.discussion.statements/pin))
                      :body-params {:share-hash "cat-dog-hash"
                                    :edit-hash "cat-dog-edit-hash"
                                    :statement-id statement-id
                                    :pin? true}}
                     toolbelt/add-csrf-header
                     (toolbelt/mock-authorization-header %)
                     test-app)]
    (is (= 200 (:status (request toolbelt/token-schnaqqifant-user))))
    (is (= 403 (:status (request toolbelt/token-wegi-no-beta-user))))))

;; -----------------------------------------------------------------------------

(defn- upload-file-request [file share-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.discussion.upload/file))
       :body-params {:file file
                     :bucket :schnaq/media
                     :share-hash share-hash}}
      toolbelt/add-csrf-header
      test-app
      m/decode-response-body))

(deftest upload-file-test
  (testing "Valid files can be uploaded."
    (is (string? (:url (upload-file-request file shared-config/allowed-share-hash-in-development))))))

(deftest upload-image-test
  (testing "Valid images can be uploaded."
    (is (string? (:url (upload-file-request image shared-config/allowed-share-hash-in-development))))))

(deftest upload-file-wrong-hash-test
  (testing "Valid files must be uploaded to valid discussions."
    (let [response (upload-file-request file (str (random-uuid)))]
      (is (nil? (:url response)))
      (is (:error response)))))

;; -----------------------------------------------------------------------------

(defn- set-focus-request [entity-id share-hash edit-hash]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.discussion.manage/focus))
       :body-params {:entity-id entity-id
                     :share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header toolbelt/token-schnaqqifant-user)
      test-app
      :status))

(deftest set-focus-test
  (let [share-hash "cat-dog-hash"
        poll-id (:db/id (first (poll-db/polls share-hash)))]
    (testing "Users with moderator rights can toggle the focus."
      (is (= 200 (set-focus-request poll-id share-hash "cat-dog-edit-hash"))))))

(deftest delete-statement-and-children!-test
  (let [parent-id (:db/id (first (discussion-db/statements-by-content "we should get a cat")))
        delete-fn
        #(-> {:request-method :delete :uri (:path (api/route-by-name :api.discussion.statements/delete))
              :body-params {:statement-id %
                            :share-hash "cat-dog-hash"
                            :edit-hash "cat-dog-edit-hash"}}
             toolbelt/add-csrf-header
             (toolbelt/mock-authorization-header toolbelt/token-schnaqqifant-user)
             test-app)]
    (testing "Are all children-statements deleted?"
      (delete-fn parent-id)
      (is (empty? (discussion-db/statements-by-content "we should get a cat")))
      (is (empty? (discussion-db/statements-by-content "cats are very independent")))
      (is (empty? (discussion-db/statements-by-content "this is not true for overbred races")))
      (is (empty? (discussion-db/statements-by-content "this lies in their natural conditions"))))
    (testing "With wrong statement-id"
      (is (= 403 (:status (delete-fn (- parent-id 999))))))))
