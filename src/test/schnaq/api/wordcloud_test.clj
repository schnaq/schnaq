(ns schnaq.api.wordcloud-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

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
      test-app
      :status))

(deftest toggle-wordcloud-test
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
                true)))
    (is (= 200 (toggle-wordcloud-request
                toolbelt/token-n2o-admin
                test-share-hash
                test-edit-hash
                false)))))

(deftest get-local-wordclouds-test
  (let [response (-> {:request-method :get :uri (:path (api/route-by-name :wordcloud/local))
                      :query-params {:share-hash "simple-hash"}}
                     test-app
                     m/decode-response-body)
        wordclouds (:wordclouds response)
        detailed-wordcloud (first (filter #(= "Nonsense" (:wordcloud/title %)) wordclouds))]
    (testing "Were the correct word clouds returned?"
      (is (= 2 (count wordclouds)))
      (is (= 4 (count (:wordcloud/words detailed-wordcloud))))
      (is (= (set [["foo" 13] ["fooo" 1] ["foobar" 5] ["barbar" 7]])
             (set (:wordcloud/words detailed-wordcloud)))))))
