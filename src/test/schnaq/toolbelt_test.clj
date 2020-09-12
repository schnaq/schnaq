(ns schnaq.toolbelt-test
  (:require [clojure.test :refer [deftest testing are is]]
            [dialog.test.toolbelt :as test-toolbelt]
            [schnaq.toolbelt :refer [pull-key-up]]))

(deftest pull-key-up-test
  (testing "No specified keys should be present anymore if the keys are correctly chosen."
    (are [x y] (= x y)
               {:foo :bar}
               (pull-key-up {:foo {:db/ident :bar}} :db/ident)
               {:foo :bar, :baz {:some-key :oof}}
               (pull-key-up {:foo {:db/ident :bar}, :baz {:some-key :oof}} :db/ident)))
  (testing "If the keys could not be found, there should still be the original map."
    (are [x y] (= x y)
               {:foo #:db{:ident :bar}}
               (pull-key-up {:foo {:db/ident :bar}} :non-existent)))
  (testing "Generative tests."
    (is (test-toolbelt/check? `pull-key-up))))

