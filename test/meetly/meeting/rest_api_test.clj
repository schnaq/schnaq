(ns meetly.meeting.rest-api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [meetly.meeting.rest-api :as api]
            [meetly.meeting.database :as db]))

(use-fixtures :each meetly-toolbelt/init-test-delete-db-fixture)

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
          old-meeting (db/meeting old-meeting-id)
          update-meeting @#'api/update-meeting!
          new-meeting-request {:body-params {:nickname new-author
                                             :meeting {:db/id old-meeting-id
                                                       :meeting/title new-title
                                                       :meeting/share-hash "Velma"
                                                       :meeting/edit-hash "Shaggy"
                                                       :meeting/start-date (db/now)
                                                       :meeting/end-date (db/now)}
                                             :agendas []}}
          update-response (update-meeting new-meeting-request)
          new-meeting (db/meeting old-meeting-id)]
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