(ns schnaq.api.wordcloud-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def test-share-hash "cat-dog-hash")
(def test-edit-hash "cat-dog-edit-hash")
(def wrong-edit-hash "wrong-edit-hash")

(defn- toggle-wordcloud-request [user-token share-hash edit-hash display-wordcloud?]
  (-> {:request-method :put :uri (:path (api/route-by-name :wordcloud/display))
       :body-params {:display-wordcloud? display-wordcloud?
                     :share-hash share-hash
                     :edit-hash edit-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      api/app
      :status))

(deftest display-word-cloud-test
  (testing "Non Pro User can't toggle wordcloud."
    (is (= 403 (toggle-wordcloud-request
                toolbelt/token-wegi-no-beta-user
                test-share-hash
                test-edit-hash
                true))))
  (testing "Pro User without edit hash can't toggle wordcloud."
    (is (= 403 (toggle-wordcloud-request
                toolbelt/token-schnaqqifant-user
                test-share-hash
                wrong-edit-hash
                true))))
  (testing "Admin and Pro User can toggle wordcloud."
    (is (= 200 (toggle-wordcloud-request
                toolbelt/token-n2o-admin
                test-share-hash
                test-edit-hash
                true)))))