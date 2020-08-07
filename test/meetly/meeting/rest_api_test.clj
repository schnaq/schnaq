(ns meetly.meeting.rest-api-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [meetly.test.toolbelt :as meetly-toolbelt]
            [meetly.meeting.rest-api :as api]))

(use-fixtures :each meetly-toolbelt/init-db-test-fixture)

(deftest add-agendas-test
  (testing "Test whether the agenda is correctly validated before passed on."
    (let [add-agendas @#'api/add-agendas
          valid-req {:body-params {:meeting-id 123
                                   :meeting-hash "asjd8394h-23d"
                                   :agendas []}}
          valid-response (add-agendas valid-req)
          invalid-req {:body-params {:meeting-id nil
                                     :meeting-hash :not-valid
                                     :agendas []}}
          invalid-response (add-agendas invalid-req)]
      (is (= 200 (:status valid-response)))
      (is (= "Agendas sent over successfully" (-> valid-response :body :text)))
      (is (= 400 (:status invalid-response)))
      (is (= "Your request was invalid" (-> invalid-response :body :error))))))

