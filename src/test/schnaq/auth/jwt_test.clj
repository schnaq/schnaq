(ns schnaq.auth.jwt-test
  (:require [buddy.core.keys :as keys]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing]]
            [schnaq.auth.jwt :as auth-jwt]
            [schnaq.test.toolbelt :refer [token-schnaqqifant-user]]))

(def testing-private-key (keys/str->private-key (slurp "https://s3.disqtec.com/on-premise/testing/jwt.key")))
(def testing-public-key (keys/str->public-key (slurp "https://s3.disqtec.com/on-premise/testing/jwt.key.pub")))

(deftest jwt-creation-and-validation-test
  (let [payload {:foo "bar"}
        jwt-token (auth-jwt/create-signed-jwt payload testing-private-key)]
    (testing "Data is converted to a signed JWT token."
      (is (string? jwt-token))
      (is (= 3 (count (string/split jwt-token #"\.")))))
    (testing "Validating and converting signed jwt results in original payload."
      (is (= payload (auth-jwt/validate-signed-jwt jwt-token testing-public-key))))
    (testing "Fails if signature does not the keys."
      (is (= :signature (try
                          (auth-jwt/validate-signed-jwt token-schnaqqifant-user testing-public-key)
                          (catch Exception e
                            (:cause (ex-data e)))))))))

