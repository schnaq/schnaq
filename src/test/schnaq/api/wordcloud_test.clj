(ns schnaq.api.wordcloud-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(def test-share-hash "cat-dog-hash")

(defn- toggle-wordcloud-request [user-token share-hash display-wordcloud?]
  (-> {:request-method :put :uri (:path (api/route-by-name :wordcloud/display))
       :body-params {:display-wordcloud? display-wordcloud?
                     :share-hash share-hash}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      :status))

(deftest toggle-wordcloud-test
  (testing "Non Pro User can't toggle wordcloud."
    (is (= 403 (toggle-wordcloud-request
                toolbelt/token-wegi-no-pro-user
                test-share-hash
                true))))
  (testing "Pro User without moderator rights can't toggle wordcloud."
    (is (= 403 (toggle-wordcloud-request
                toolbelt/token-schnaqqifant-user
                test-share-hash
                true))))
  (testing "Moderator and Pro User can toggle wordcloud."
    (is (= 200 (toggle-wordcloud-request
                toolbelt/token-n2o-admin
                test-share-hash
                true)))
    (is (= 200 (toggle-wordcloud-request
                toolbelt/token-n2o-admin
                test-share-hash
                false)))))
