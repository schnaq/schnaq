(ns schnaq.api.subscription.stripe-test
  (:require
   [clojure.test :refer [deftest is]]
   [schnaq.api.subscription.stripe :as subject]))

(deftest create-checkout-session-test
  (is (= true
         (subject/foo))))
