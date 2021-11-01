(ns schnaq.api.discussion-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.api :as api]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.test.toolbelt :as toolbelt]))

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
      (println (slurp (-> "beta-tester" request api/app :body)))
      (is (= 200 (-> toolbelt/token-schnaqqifant-user request api/app :status)))
      (is (= 403 (-> toolbelt/token-wegi-no-beta-user request api/app :status))))))
