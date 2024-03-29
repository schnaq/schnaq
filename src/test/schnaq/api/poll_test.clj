(ns schnaq.api.poll-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [muuntaja.core :as m]
            [schnaq.api :as api]
            [schnaq.database.poll :as poll-db]
            [schnaq.test.toolbelt :as toolbelt :refer [test-app]]))

(use-fixtures :each toolbelt/init-test-delete-db-fixture)
(use-fixtures :once toolbelt/clean-database-fixture)

(deftest polls-for-discussion-test
  (testing "Return all polls. Needs nothing except the share-hash."
    (let [functioning-hash "cat-dog-hash"
          request (fn [share-hash] {:request-method :get
                                    :uri "/polls"
                                    :query-params {:share-hash share-hash}})]
      (is (= 200 (-> functioning-hash request test-app :status)))
      (is (= 3 (count (-> functioning-hash request test-app m/decode-response-body :polls))))
      (is (empty? (-> "some-dingus-hash" request test-app m/decode-response-body :polls))))))

(deftest new-poll-test
  (let [share-hash "cat-dog-hash"
        request (fn [user-token]
                  (-> {:request-method :post
                       :uri "/poll"
                       :body-params {:title "New Poll"
                                     :poll-type :poll.type/single-choice
                                     :options ["a" "b" "c"]
                                     :share-hash share-hash
                                     :hide-results? false}}
                      toolbelt/add-csrf-header
                      (toolbelt/mock-authorization-header user-token)))]
    (testing "Non logged in user can not create a poll."
      (is (= 403 (-> toolbelt/token-timed-out request test-app :status))))
    (testing "Logged in user without pro cannot create a poll."
      (is (= 403 (-> toolbelt/token-wegi-no-pro-user request test-app :status))))
    (testing "Pro user, that has no moderation rights cannot create poll."
      (is (= 403 (-> toolbelt/token-schnaqqifant-user request test-app :status))))
    (testing "User with correct pro status and moderation rights, has provided no options."
      (is (= 400 (-> toolbelt/token-n2o-admin request (assoc-in [:body-params :options] [])
                     test-app :status))))
    (testing "Adding a poll is allowed for the pro user with correct params."
      (is (= 200 (-> toolbelt/token-n2o-admin request test-app :status)))
      (is (= 4 (count (poll-db/polls share-hash)))))))

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
      (is (= 200 (-> option-id request test-app :status)))
      (is (-> option-id request test-app m/decode-response-body :voted?))
      (is (= 400 (-> 1 request test-app :status)))
      (testing "Cast multiple votes for a multiple choice poll"
        (is (= 200 (-> option-ids multiple-request test-app :status)))
        (is (-> option-ids multiple-request test-app m/decode-response-body :voted?))))))

;; -----------------------------------------------------------------------------

(defn- delete-poll-request [poll-id user-token]
  (-> {:request-method :delete :uri (:path (api/route-by-name :poll/delete))
       :body-params {:poll-id poll-id
                     :share-hash "cat-dog-hash"}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      :status))

(deftest delete-poll-test
  (let [poll-id (-> (poll-db/polls "cat-dog-hash") first :db/id)]
    (testing "Delete polls with valid credentials."
      (is (= 200 (delete-poll-request poll-id toolbelt/token-n2o-admin)))
      (is (= 200 (delete-poll-request poll-id toolbelt/token-kangaroo-normal-user)))
      (is (= 200 (delete-poll-request poll-id toolbelt/token-wegi-no-pro-user))))))

;; -----------------------------------------------------------------------------

(defn- get-poll-request [share-hash poll-id]
  (-> {:request-method :get :uri (:path (api/route-by-name :api/poll))
       :query-params {:poll-id poll-id
                      :share-hash share-hash}}
      test-app
      :status))

(deftest get-poll-test
  (let [share-hash "cat-dog-hash"
        poll-id (-> (poll-db/polls share-hash) first :db/id)]
    (testing "Valid share-hash and poll-id returns poll."
      (is (= 200 (get-poll-request share-hash poll-id))))
    (testing "Invalid poll-id returns bad request."
      (is (= 400 (get-poll-request share-hash (inc poll-id)))))
    (testing "Wrong share-hash returns bad request."
      (is (= 400 (get-poll-request "something different" poll-id))))))

;; -----------------------------------------------------------------------------

(defn- toggle-hide-results-request [share-hash poll-id hide-results? user-token]
  (-> {:request-method :put :uri (:path (api/route-by-name :api.poll/hide-results))
       :body-params {:poll-id poll-id
                     :share-hash share-hash
                     :hide-results? hide-results?}}
      toolbelt/add-csrf-header
      (toolbelt/mock-authorization-header user-token)
      test-app
      :status))

(deftest toggle-hide-results-test
  (let [share-hash "cat-dog-hash"
        poll-id (-> (poll-db/polls share-hash) first :db/id)]
    (testing "Toggle hide-results via api."
      (is (= 200 (toggle-hide-results-request share-hash poll-id true toolbelt/token-wegi-no-pro-user)))
      (is (= 403 (toggle-hide-results-request share-hash poll-id true toolbelt/token-schnaqqifant-user))))))
