(ns schnaq.auth.jwt-test
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [schnaq.auth.jwt :as auth-jwt]
            [schnaq.config :as config]
            [schnaq.test.toolbelt :refer [token-wrong-signature]]))

(deftest jwt-creation-and-validation-test
  (let [payload {:foo "bar"}
        jwt-token (auth-jwt/create-signed-jwt payload config/testing-private-key)]
    (testing "Data is converted to a signed JWT token."
      (is (string? jwt-token))
      (is (= 3 (count (string/split jwt-token #"\.")))))
    (testing "Validating and converting signed jwt results in original payload."
      (is (= payload (auth-jwt/validate-signed-jwt jwt-token config/testing-public-key))))
    (testing "Fails if wrong signature is provided."
      (is (= :signature (try
                          (auth-jwt/validate-signed-jwt token-wrong-signature config/testing-public-key)
                          (catch Exception e
                            (:cause (ex-data e)))))))))

