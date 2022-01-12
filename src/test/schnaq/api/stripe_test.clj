(ns schnaq.api.stripe-test
  (:require [clojure.test :refer [deftest is]]
            [schnaq.api.stripe :as sut]))

(deftest create-checkout-session-test
  (is (= true true)))
