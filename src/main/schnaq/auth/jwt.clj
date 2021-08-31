(ns schnaq.auth.jwt
  (:require [buddy.sign.jws :as jws]
            [buddy.sign.jwt :as jwt]
            [muuntaja.core :as m]))

(defn create-signed-jwt
  "Takes a private key and a payload to create a signed JWT Token."
  [payload private-key]
  (jws/sign (slurp (m/encode "application/json" payload))
            private-key
            {:alg :rs256}))

(defn validate-signed-jwt
  "Takes a signed JWT and validates the signature. Returns the decoded payload."
  [jwt public-key]
  (jwt/unsign jwt public-key {:alg :rs256}))
