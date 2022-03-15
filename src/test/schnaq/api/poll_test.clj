(ns schnaq.api.poll-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.poll :as poll-db]
            [schnaq.test.toolbelt :as toolbelt]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest polls-for-discussion-test
  (testing "Return all polls. Needs nothing except the share-hash."
    (let [functioning-hash "cat-dog-hash"
          request (fn [share-hash] {:request-method :get
                                    :uri "/polls"
                                    :query-params {:share-hash share-hash}})]
      (is (= 200 (-> functioning-hash request api/app :status)))
      (is (= 2 (count (-> functioning-hash request api/app m/decode-response-body :polls))))
      (is (empty? (-> "some-dingus-hash" request api/app m/decode-response-body :polls))))))

(deftest new-poll-test
  (let [share-hash "cat-dog-hash"
        request (fn [user-token]
                  (-> {:request-method :post
                       :uri "/poll"
                       :body-params {:title "New Poll"
                                     :poll-type :poll.type/single-choice
                                     :options ["a" "b" "c"]
                                     :share-hash share-hash
                                     :edit-hash "cat-dog-edit-hash"}}
                      toolbelt/add-csrf-header
                      (toolbelt/mock-authorization-header user-token)))]
    (testing "Non logged in user can not create a poll."
      (is (= 401 (-> toolbelt/token-timed-out request api/app :status))))
    (testing "Logged in user without pro cannot create a poll."
      (is (= 403 (-> toolbelt/token-wegi-no-beta-user request api/app :status))))
    (testing "Pro user, that has wrong admin credentials cannot create poll."
      (is (= 403 (-> toolbelt/token-schnaqqifant-user request (assoc-in [:body-params :edit-hash] "wrong-edit")
                     api/app :status))))
    (testing "User with correct pro status, credentials and admin, has provided no options."
      (is (= 400 (-> toolbelt/token-schnaqqifant-user request (assoc-in [:body-params :options] [])
                     api/app :status))))
    (testing "Adding a poll is allowed for the pro user with correct params."
      (is (= 200 (-> toolbelt/token-schnaqqifant-user request api/app :status)))
      (is (= 3 (count (poll-db/polls share-hash)))))))

(deftest cast-vote-test
  (testing "Casting a vote works always, as long as poll-id, option-id and discussion share-hash match."
    (let [share-hash "simple-hash"
          multiple-hash "cat-dog-hash"
          poll (first (poll-db/polls share-hash))
          multi-poll (first (filter #(= :poll.type/multiple-choice (:poll/type %))
                                    (poll-db/polls multiple-hash)))
          option-id (-> poll :poll/options first :db/id)
          option-ids (->> multi-poll :poll/options (map :db/id))
          request (fn [option-id]
                    (-> {:request-method :put
                         :uri (format "/poll/%s/vote" (:db/id poll))
                         :body-params {:share-hash share-hash
                                       :option-id option-id}}
                        toolbelt/add-csrf-header))
          multiple-request (fn [option-ids]
                             (-> {:request-method :put
                                  :uri (format "/poll/%s/vote" (:db/id multi-poll))
                                  :body-params {:share-hash multiple-hash
                                                :option-id option-ids}}
                                 toolbelt/add-csrf-header))]
      (is (= 200 (-> option-id request api/app :status)))
      (is (-> option-id request api/app m/decode-response-body :voted?))
      (is (= 400 (-> 1 request api/app :status)))
      (testing "Cast multiple votes for a multiple choice poll"
        (is (= 200 (-> option-ids multiple-request api/app :status)))
        (is (-> option-ids multiple-request api/app m/decode-response-body :voted?))))))

;; -----------------------------------------------------------------------------

(defn- delete-poll-request [poll-id user-token]
  (-> {:request-method :delete :uri (:path (api/route-by-name :poll/delete))
       :body-params {:poll-id poll-id
                     :share-hash "cat-dog-hash"
                     :edit-hash "cat-dog-edit-hash"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      api/app
      :status))

(deftest delete-poll-test
  (let [poll-id (-> (poll-db/polls "cat-dog-hash") first :db/id)]
    (testing "Delete polls with valid credentials."
      (is (= 200 (delete-poll-request poll-id toolbelt/token-n2o-admin)))
      (is (= 200 (delete-poll-request poll-id toolbelt/token-schnaqqifant-user)))
      (is (= 200 (delete-poll-request poll-id toolbelt/token-wegi-no-beta-user))))))
