(ns schnaq.toolbelt-test
  (:require [clojure.test :refer [deftest testing are is]]
            [schnaq.test.toolbelt :as test-toolbelt]
            [schnaq.toolbelt :refer [pull-key-up build-allowed-origin]]))

(deftest pull-key-up-test
  (testing "No specified keys should be present anymore if the keys are correctly chosen."
    (are [x y] (= x y)
               {:foo :bar}
               (pull-key-up {:foo {:db/ident :bar}})
               {:foo :bar, :baz {:some-key :oof}}
               (pull-key-up {:foo {:db/ident :bar}, :baz {:some-key :oof}} :db/ident)))
  (testing "If the keys could not be found, there should still be the original map."
    (are [x y] (= x y)
               {:foo #:db{:ident :bar}}
               (pull-key-up {:foo {:db/ident :bar}} :non-existent)))
  (testing "Generative tests."
    (is (test-toolbelt/check? `pull-key-up))))

(deftest build-allowed-origin-test
  (testing "Building valid patterns"
    (are [string url result]
      (= result (not (nil? (re-find (build-allowed-origin string) url))))
      "localhost:8700" "http://localhost:8700" true
      "localhost:8700" "http://localhost:3000" false
      "schnaq.com" "http://schnaq.com" true
      "schnaq.com" "https://schnaq.com" true
      "schnaq.com" "https://api.schnaq.com" true
      "schnaq.com" "https://api.razupaltuff.schnaq.com" true
      "schnaq.com" "https://schnaqsel.com" false
      "penguin.books" "https://penguin.books" true)))
