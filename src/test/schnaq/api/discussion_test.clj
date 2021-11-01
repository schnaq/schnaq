(ns schnaq.api.discussion-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ring.mock.request :as mock]
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

(comment
  (def foo
    {:label ":check"
     :display-name "A. Schneider"
     :share-hash "63107b37-899d-423a-b117-3514105d2319"
     :statement-id 17592186051114})
  (def rq #(-> {:request-method :put :uri "/discussion/statement/label/add"}
               (assoc-in [:identity :roles] #{%})
               (assoc-in [:identity :sub] "59456d4a-6950-47e8-88d8-a1a6a8de9276")
               (assoc :body-params foo)
               toolbelt/add-csrf-header))
  (api/app (rq "beta-tester"))
  )
