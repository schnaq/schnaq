(ns schnaq.api.moderation-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [muuntaja.core :as m]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest load-moderators-test
  (testing "Return all moderators. Should be two overall."
    (let [response (-> {:request-method :get
                        :uri "/moderation/moderators"
                        :query-params {:share-hash "cat-dog-hash"}}
                       (toolbelt/mock-authorization-header toolbelt/token-n2o-admin)
                       toolbelt/test-app)
          moderators (-> response m/decode-response-body :moderators)]
      (is (= 200 (-> response :status)))
      (is (= 2 (count moderators)))
      (is (some #{"k@ngar.oo"} moderators))
      (is (some #{"christian@schnaq.com"} moderators)))))

(defn demote-moderator-request
  [token]
  (-> {:request-method :post
       :uri "/moderation/demote"
       :body-params {:share-hash "cat-dog-hash"
                     :email "k@ngar.oo"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header token)
      toolbelt/test-app))

(deftest demote-moderator-test
  (testing "Schnaq owner can remove the moderator without problems."
    (let [response (demote-moderator-request toolbelt/token-wegi-no-pro-user)
          demoted? (-> response m/decode-response-body :demoted?)]
      (is (= 200 (-> response :status)))
      (is demoted?))))

(deftest demote-moderator-unauthorized-test
  (testing "Non owners can not remove a moderator from the schnaq."
    (let [response (demote-moderator-request toolbelt/token-n2o-admin)]
      (is (= 400 (-> response :status))))))
