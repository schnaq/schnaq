(ns schnaq.auth
  (:require [buddy.core.codecs.base64 :as b64]
            [buddy.sign.jwt :as jwt]
            [buddy.sign.jwe :as jwe]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn >defn-]]
            [ring.util.http-response :refer [ok bad-request unauthorized]]
            [schnaq.config :as config]
            [clojure.spec.alpha :as s]))

(s/def :token/payload map?)
(s/def :token/header map?)

;; -----------------------------------------------------------------------------
;; Token extraction

(>defn- has-token?
  "Check if the request headers for the tokens are set."
  [request]
  [map? :ret boolean?]
  (string? (get (:headers request) "authorization")))

(defn- extract-token-from-request
  "Extract token from request header."
  [request]
  (second (string/split (get (:headers request) "authorization") #" ")))

(defn- valid-token?
  "Check the token's signature and that the token is still valid."
  ([token]
   (valid-token? token config/keycloak-public-key))
  ([token public-key]
   (try
     (jwt/unsign token public-key {:alg :rs256})
     (catch Throwable t
       {:error? true
        :message (ex-message t)}))))

(>defn- decode-b64
  "Decode a base64-encoded string to return it as a string."
  [base64-encoded]
  [string? :ret string?]
  (String. ^bytes (b64/decode (.getBytes base64-encoded))))

(>defn decode-token
  "Decode the received JWT and split it into header and payload."
  [token]
  [string? :ret (s/keys :req [:token/header :token/payload])]
  (let [[header payload _] (clojure.string/split token #"\.")]
    {:token/header (json/read-str (decode-b64 header))
     :token/payload (json/read-str (decode-b64 payload))}))


;; -----------------------------------------------------------------------------
;; Token interpretation

(defn- extract-realm-roles
  "Extract the realm roles of a user."
  [token]
  (let [payload (:token/payload (decode-token token))]
    (get-in payload ["realm_access" "roles"])))

(defn- has-admin-role?
  "Look up the user's roles and return true, if the user is an admin."
  [token]
  (= "admin" (some #{"admin"} (extract-realm-roles token))))


;; -----------------------------------------------------------------------------
;; Middlewares

(defn with-valid-token
  "Validate that the user is properly logged in and can therefore provide a
  valid JWT token."
  [view request]
  (if (and (has-token? request)
           (valid-token? (extract-token-from-request request)))
    (view request)
    (bad-request "Malformed JWT token.")))

(defn with-admin-access
  "Check if request with JWT token has :is-admin? claim"
  [view request]
  (if (and (has-token? request)
           (has-admin-role? (extract-token-from-request request)))
    (view request)
    (unauthorized "You are not an admin.")))

(defn testview
  "testing"
  [request]
  (prn request)
  (def foo request)
  (def footoken (second (string/split (get (:headers request) "authorization") #" ")))
  (ok {:message "Jeaasdqweh"}))

(def testtoken "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4MzkyMTksImlhdCI6MTYxMTgzODkxOSwiYXV0aF90aW1lIjoxNjExODMxNzI2LCJqdGkiOiJiMzA3MzNiOS00NjExLTRjOGMtYjY3OS1mZmJmNDUwNDdkOTIiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImJjYWVjMDgyLTI0YTgtNDU4Mi1hY2UxLTg2NjUyOTE1YTIyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiJiMjc4MmUyYS1kMzQwLTRhZGUtOGQ0OS03N2ZhMzUxNWMzNWQiLCJzZXNzaW9uX3N0YXRlIjoiZTg1Njk4YzItYzMzMy00YjUwLWI3ZmItNDU1YWQ1NjM0OGE0IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RpYW4gTWV0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuMm8iLCJnaXZlbl9uYW1lIjoiQ2hyaXN0aWFuIiwiZmFtaWx5X25hbWUiOiJNZXRlciIsImVtYWlsIjoiY21ldGVyQGdvb2dsZW1haWwuY29tIn0.F8Pepi93FqBB6P3la7KHqFFhZ3k4EzHC2Nz-s6x8PwoyZnhgEqAsEmklNeXY4O45U2tYRVkcfTtMAo74LVa8VacRbaUoIXTq5Q3VlQNGJW35oSFNMPOSGKTfj216rj62J9utyfmF-l1bIV803xon2lQXzVAcQ3JVTbnd3T-PcpPh3YlwwiAv1kmQ-zJ6znXOFkhSmT0BCarvWyQRctQa2rNsCsKgX8LjtSnrVPeb8pxuMOYrQPuOHAJ4jtYZ4EcLU-Z7GZdG367jtQ263pR-hfxOONwy47ihRNwcZJAtjxL7emUv9UozKzldWhudliC8J2OBin-xbi460JJkE0RaFA")

(comment
  (valid-token? testtoken)
  (decode-token testtoken)
  (decode-token footoken)
  (jwe/decode-header testtoken)
  (jwe/decode-payload)


  :nil)



