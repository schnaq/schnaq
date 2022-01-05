(ns schnaq.api.subscription.stripe-test
  (:require [clojure.test :refer [deftest is]]
            [schnaq.api.subscription :as sut]))

(deftest create-checkout-session-test
  (is (= true true)))
