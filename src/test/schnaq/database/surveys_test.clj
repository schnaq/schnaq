(ns schnaq.database.surveys-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [schnaq.database.surveys :as survey-db]
            [schnaq.test-data :refer [kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest participate-using-schnaq-for-survey-test
  (testing "Participating in a survey should succeed."
    (let [keycloak-id (:user.registered/keycloak-id kangaroo)
          topics [:surveys.using-schnaq-for.topics/coachings :surveys.using-schnaq-for.topics/meetings]]
      (is (map?
           (survey-db/participate-using-schnaq-for-survey keycloak-id topics))))))

(deftest using-schnaq-for-results-test
  (testing "If there are survey results, return them."
    (survey-db/participate-using-schnaq-for-survey
     (:user.registered/keycloak-id kangaroo)
     [:surveys.using-schnaq-for.topics/meetings])
    (is (= 1 (count (survey-db/using-schnaq-for-results))))))
