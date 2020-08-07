(ns meetly.meeting.rest-api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [meetly.meeting.rest-api :as api]
            [meetly.meeting.database :as db]))

(use-fixtures :each meetly-toolbelt/init-db-test-fixture)

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

