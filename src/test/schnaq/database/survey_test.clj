(ns schnaq.database.survey-test
  (:require [clojure.test :refer [use-fixtures is deftest testing]]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.survey :as db]
            [schnaq.test.toolbelt :as schnaq-toolbelt]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(deftest new-survey-test
  (testing "Check whether the survey object is created correctly."
    (let [discussion-id (:db/id (fast-pull [:discussion/share-hash "cat-dog-hash"] '[:db/id]))
          new-survey-fn @#'db/new-survey
          new-survey (new-survey-fn "Test Survey" :survey.type/multiple-choice
                                    ["Eis" "Sorbet" "Joghurt"] discussion-id)
          failed-survey (new-survey-fn "Failed" :survey.type/single-choice
                                       [] discussion-id)]
      (is (zero? (apply + (map :option/votes (:survey/options new-survey)))))
      (is (= 3 (count (:survey/options new-survey))))
      (is (= :survey.type/multiple-choice (:survey/type new-survey)))
      (is (= "Cat or Dog?" (get-in new-survey [:survey/discussion :discussion/title])))
      (is (nil? failed-survey)))))
