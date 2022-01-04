(ns schnaq.api.survey-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.survey :as survey-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest surveys-for-discussion-test
  (testing "Return all surveys. Needs nothing except the share-hash."
    (let [functioning-hash "cat-dog-hash"
          request (fn [share-hash] {:request-method :get
                                    :uri "/surveys"
                                    :query-params {:share-hash share-hash}})]
      (is (= 200 (-> functioning-hash request api/app :status)))
      (is (= 2 (count (-> functioning-hash request api/app m/decode-response-body :surveys))))
      (is (empty? (-> "some-dingus-hash" request api/app m/decode-response-body :surveys))))))

(deftest new-survey-test
  (let [share-hash "cat-dog-hash"
        request (fn [user-token]
                  (-> {:request-method :post
                       :uri "/survey"
                       :body-params {:title "New Survey"
                                     :survey-type :survey.type/single-choice
                                     :options ["a" "b" "c"]
                                     :share-hash share-hash
                                     :edit-hash "cat-dog-edit-hash"}}
                      toolbelt/add-csrf-header
                      (toolbelt/mock-authorization-header user-token)))]
    (testing "Non logged in user can not create a survey."
      (is (= 401 (-> toolbelt/token-timed-out request api/app :status))))
    (testing "Logged in user without premium can not create a survey."
      (is (= 403 (-> toolbelt/token-wegi-no-beta-user request api/app :status))))
    (testing "Premium user, that has wrong admin credentials cannot create survey."
      (is (= 403 (-> toolbelt/token-schnaqqifant-user request (assoc-in [:body-params :edit-hash] "wrong-edit")
                     api/app :status))))
    (testing "User with correct pro status, credentials and admin, has provided no options."
      (is (= 400 (-> toolbelt/token-schnaqqifant-user request (assoc-in [:body-params :options] [])
                     api/app :status))))
    (testing "Adding a survey is allowed for the pro user with correct params."
      (is (= 200 (-> toolbelt/token-schnaqqifant-user request api/app :status)))
      (is (= 3 (count (survey-db/surveys share-hash)))))))

(deftest cast-vote-test
  (testing "Casting a vote works always, as long as survey-id, option-id and discussion share-hash match."
    (let [share-hash "simple-hash"
          survey (first (survey-db/surveys share-hash))
          option-id (-> survey :survey/options first :db/id)
          request (fn [option-id]
                    (-> {:request-method :put
                         :uri (format "/survey/%s/vote" (:db/id survey))
                         :body-params {:share-hash share-hash
                                       :option-id option-id}}
                        toolbelt/add-csrf-header))]
      (is (= 200 (-> option-id request api/app :status)))
      (is (-> option-id request api/app m/decode-response-body :voted?))
      (is (= 400 (-> 1 request api/app :status))))))
