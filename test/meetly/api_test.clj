(ns meetly.api-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [meetly.api :as api]
            [meetly.meeting.database :as db]
            [meetly.meeting.models :as models]
            [meetly.test.toolbelt :as meetly-toolbelt]))

(use-fixtures :each meetly-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once meetly-toolbelt/clean-database-fixture)

(deftest add-meeting-with-empty-description-test
  (testing "Check whether a meeting with an empty description is added or refused."
    (let [response (@#'api/add-meeting {:body-params {:meeting {:meeting/title "Test"
                                                                :meeting/start-date (db/now)
                                                                :meeting/end-date (db/now)
                                                                :meeting/description ""}
                                                      :nickname "Wegi"}})]
      (is (= 201 (:status response)))
      (is (s/valid? ::models/meeting (-> response :body :new-meeting))))))

(deftest add-agendas-test
  (testing "Test whether the agenda is correctly validated before passed on."
    (let [add-agendas @#'api/add-agendas
          add-meeting (db/add-meeting {:meeting/title "foo"
                                       :meeting/share-hash "abbada"
                                       :meeting/start-date (db/now)
                                       :meeting/end-date (db/now)
                                       :meeting/author (db/add-user-if-not-exists "Wegi")})
          valid-req {:body-params {:meeting-id add-meeting
                                   :meeting-hash "abbada"
                                   :agendas []}}
          valid-response (add-agendas valid-req)
          invalid-req {:body-params {:meeting-id nil
                                     :meeting-hash :not-valid
                                     :agendas []}}
          invalid-response (add-agendas invalid-req)]
      (is (= 200 (:status valid-response)))
      (is (= "Agendas sent over successfully" (-> valid-response :body :text)))
      (is (= 400 (:status invalid-response)))
      (is (= "Agenda could not be added" (-> invalid-response :body :error))))))

(deftest update-meeting-test
  (testing "Test whether a meeting updates correctly"
    (let [new-title "foo Neu"
          new-author "Der Schredder"
          old-share-hash "abbada"
          old-edit-hash "Scooby Doo"
          old-meeting-id (db/add-meeting {:meeting/title "foo"
                                          :meeting/share-hash old-share-hash
                                          :meeting/edit-hash old-edit-hash
                                          :meeting/start-date (db/now)
                                          :meeting/end-date (db/now)
                                          :meeting/author (db/add-user-if-not-exists "Wegi")})
          old-meeting (db/meeting-private-data old-meeting-id)
          update-meeting @#'api/update-meeting!
          new-meeting-request {:body-params {:nickname new-author
                                             :meeting {:db/id old-meeting-id
                                                       :meeting/title new-title
                                                       :meeting/share-hash old-share-hash
                                                       :meeting/edit-hash old-edit-hash
                                                       :meeting/start-date (db/now)
                                                       :meeting/end-date (db/now)}
                                             :agendas []}}
          update-response (update-meeting new-meeting-request)
          new-meeting (db/meeting-private-data old-meeting-id)]
      (testing "Check response status"
        (is (= 200 (:status update-response)))
        (is (= "Your Meetly has been updated." (-> update-response :body :text))))
      (testing "Check if title and author have been updated"
        (is (not= (:meeting/title old-meeting) (:meeting/title new-meeting)))
        (is (not= (:meeting/author old-meeting) (:meeting/author new-meeting))))
      (testing "Check if title and author have been updated correctly"
        (is (= new-title (:meeting/title new-meeting)))
        (is (= (db/user-by-nickname new-author) (:db/id (:meeting/author new-meeting)))))
      (testing "Check whether forbidden attributes stayed the same"
        (is (= old-share-hash (:meeting/share-hash new-meeting)))
        (is (= old-edit-hash (:meeting/edit-hash new-meeting)))))))

(deftest update-meeting-invalid-test
  (testing "Test if invalid credentials are caught"
    (let [new-title "foo Neu"
          new-author "Der Schredder"
          old-share-hash "abbada"
          old-edit-hash "Scooby Doo"
          old-meeting-id (db/add-meeting {:meeting/title "foo"
                                          :meeting/share-hash old-share-hash
                                          :meeting/edit-hash old-edit-hash
                                          :meeting/start-date (db/now)
                                          :meeting/end-date (db/now)
                                          :meeting/author (db/add-user-if-not-exists "Wegi")})
          update-meeting @#'api/update-meeting!
          new-meeting-request {:body-params {:nickname new-author
                                             :meeting {:db/id old-meeting-id
                                                       :meeting/title new-title
                                                       :meeting/share-hash old-share-hash
                                                       :meeting/edit-hash "i am invalid xD"
                                                       :meeting/start-date (db/now)
                                                       :meeting/end-date (db/now)}
                                             :agendas []}}
          update-response (update-meeting new-meeting-request)
          new-meeting (db/meeting-private-data old-meeting-id)]
      (is (= old-share-hash (:meeting/share-hash new-meeting)))
      (is (= old-edit-hash (:meeting/edit-hash new-meeting)))
      (is (= 400 (:status update-response)))
      (is (= "You are not allowed to update this meeting." (-> update-response :body :error))))))

(deftest check-credentials-test
  (testing "Check if credentials are verified correctly."
    (let [check-credentials @#'api/check-credentials
          share-hash "abbada"
          edit-hash "Scooby Doo"
          _ (db/add-meeting {:meeting/title "foo"
                             :meeting/share-hash share-hash
                             :meeting/edit-hash edit-hash
                             :meeting/start-date (db/now)
                             :meeting/end-date (db/now)
                             :meeting/author (db/add-user-if-not-exists "Wegi")})
          succeeding-response (check-credentials {:body-params {:share-hash share-hash :edit-hash edit-hash}})
          failing-response (check-credentials {:body-params {:share-hash share-hash :edit-hash "INVALID"}})]
      (is (= 200 (:status succeeding-response)))
      (is (-> succeeding-response :body :valid-credentials?))
      (is (not (-> failing-response :body :valid-credentials?)))
      (is (= 200 (:status failing-response))))))