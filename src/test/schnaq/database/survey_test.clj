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
          new-survey (db/new-survey! "Test Survey" :survey.type/multiple-choice
                                     ["Eis" "Sorbet" "Joghurt"] discussion-id)
          failed-survey (db/new-survey! "Failed" :survey.type/single-choice
                                        [] discussion-id)]
      (is (zero? (apply + (map :option/votes (:survey/options new-survey)))))
      (is (= 3 (count (:survey/options new-survey))))
      (is (= :survey.type/multiple-choice (:survey/type new-survey)))
      (is (= "Cat or Dog?" (get-in new-survey [:survey/discussion :discussion/title])))
      (is (nil? failed-survey)))))

(deftest surveys-test
  (testing "Correctly retrieve all surveys for cat-dog-discussion"
    (let [surveys (db/surveys "cat-dog-hash")
          single (first (filter #(= :survey.type/single-choice (:survey/type %)) surveys))
          multiple (first (filter #(= :survey.type/multiple-choice (:survey/type %)) surveys))]
      (is (= 2 (count surveys)))
      (is (= 3 (count (:survey/options single)) (count (:survey/options multiple))))
      (is (= "Ganz allein" (:survey/title single)))
      (is (= "Ganz allein mit mehreren" (:survey/title multiple)))
      (is (= 4 (apply + (map :option/votes (:survey/options single)))))
      (is (= 3 (apply + (map :option/votes (:survey/options multiple))))))))
