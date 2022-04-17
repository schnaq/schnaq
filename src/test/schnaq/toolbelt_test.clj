(ns schnaq.toolbelt-test
  (:require [clojure.test :refer [deftest testing are]]
            [schnaq.toolbelt :refer [build-allowed-origin]]))

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
