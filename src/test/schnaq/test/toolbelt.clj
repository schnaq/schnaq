(ns schnaq.test.toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [datomic.api :as datomic]
            [expound.alpha :as expound]
            [ring.mock.request :as mock]
            [schnaq.api :as api]
            [schnaq.auth.jwt :as sjwt]
            [schnaq.config :as config]
            [schnaq.database.main :as database]
            [schnaq.test-data :as test-data]))

;; -----------------------------------------------------------------------------
;; Fixtures
(def ^:private datomic-test-uri "datomic:mem://test-db")

(defn clean-database-fixture
  "Cleans the database. Should be used with :once to start with a clean sheet."
  [f]
  (datomic/delete-database datomic-test-uri)
  (f))

(defn init-db-test-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  ([f]
   (database/init-and-seed! datomic-test-uri)
   (f))
  ([f test-data]
   (database/init-and-seed! datomic-test-uri test-data)
   (f)))

;; A bug in kaocha seems to try to interpret the result of the fixture and not of the test. Which means, until it is
;; fixed, we need to return the result of f and not end on any other function call.
(defn init-test-delete-db-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  ([f]
   (let [result (init-db-test-fixture f)]
     (datomic/delete-database datomic-test-uri)
     result))
  ([f test-data]
   (let [result (init-db-test-fixture f test-data)]
     (datomic/delete-database datomic-test-uri)
     result)))

;; -----------------------------------------------------------------------------
;; Generative Test Helpers

(defn- passed-all-tests?
  "`check` returns a list of tests. Get all results of these tests and check
  that they are all true."
  [results]
  (every? true?
          (map #(get-in % [:clojure.spec.test.check/ret :pass?]) results)))

(defn check?
  "Given a fully qualified function name, apply generative tests and pretty
  print the results, if there are any errors."
  [fn]
  (binding [s/*explain-out* expound/printer]
    (let [test-results (stest/check fn)]
      (if (passed-all-tests? test-results)
        true
        (do (expound/explain-results test-results) false)))))

;; -----------------------------------------------------------------------------
;; Authentication test helper
(defn jwt-from-test-user
  "Create JWT token for our test users, defined in `test_data.clj`."
  [{:user.registered/keys [display-name email last-name first-name roles keycloak-id]}]
  {:exp 5950068575100 :iat 1630069374 :auth_time 1630069374 :jti 1
   :aud "account" :type "Bearer"
   :realm_access {:roles (map name roles)}
   :scope "email profile"
   :avatar (format "https://schnaq.com/user/%s/avatar" display-name)
   :name first-name
   :given_name (format "%s %s" first-name last-name)
   :preferred_username display-name
   :family_name last-name
   :email email
   :sub keycloak-id})

(def token-schnaqqifant-user
  (sjwt/create-signed-jwt (jwt-from-test-user test-data/schnaqqi) config/testing-private-key))
(def token-n2o-admin
  (sjwt/create-signed-jwt (jwt-from-test-user test-data/christian) config/testing-private-key))
(def token-timed-out
  (let [payload (jwt-from-test-user test-data/christian)]
    (sjwt/create-signed-jwt (assoc payload :exp (inc (:iat payload))) config/testing-private-key)))
(def token-wegi-no-pro-user
  (sjwt/create-signed-jwt (jwt-from-test-user test-data/alex) config/testing-private-key))
(def token-kangaroo-normal-user
  (sjwt/create-signed-jwt (jwt-from-test-user test-data/kangaroo) config/testing-private-key))
(def token-wrong-signature
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4NTE4MzAsImlhdCI6MTYxMTg1MTUzMCwiYXV0aF90aW1lIjoxNjExODUxNTI1LCJqdGkiOiJiOTA3ZTJjMi0wM2FmLTQwOTAtOGI0MS0wOGFkOGIzZGUwYTYiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZmODcwOTRjLTRhNjctNDdlOC04NTQzLTQ2ZTAwNzRkYjQ5MiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIzNjIwN2IyMS05NTY2LTRiYTItODE4NC0xOTJmMGZhZWE5MGQiLCJzZXNzaW9uX3N0YXRlIjoiMjJmNDJhYmMtMWEzMy00ODliLWIyYzktODI3YWE2M2U3YjY2IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJzY2huYXFxaSBmYW50IiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2NobmFxcWlmYW50IiwiZ2l2ZW5fbmFtZSI6InNjaG5hcXFpIiwiZmFtaWx5X25hbWUiOiJmYW50IiwiZW1haWwiOiJpbmZvQHNjaG5hcS5jb20ifQ.lupfFWowuI3TrkRsaR782R6VG6ZwRFyIoRzMCD0AXw_Xb0zoqFvaJZGoMJhzeiu5s-U2gCU8YF3UwMn3sn16oJqnGtEPWKYX_CFvYd59N5MluXJaxGtzGORsR8LfTRUKeC_RkaOe_qPBqScOsvy0gHJCtUGUlAm50vbkK6eer8GwhpclUcHaOfrhEiP5U6yzJsW-dY7rKPojwfsV_KXPFK-1BtJn_yH_Fuv7jkUcPBx86zX_iVIaZfXjo3Y24kYZuVmx6euWs2yOoSXU9RxRpTxJtBIdHC2vwmizivPdY2KGbah9CmfFqyM45kg4lY22qLssrFvlbgG-ifkLukcnTw")

;; -----------------------------------------------------------------------------

(>defn mock-authorization-header
  "Add authorization token header to a request."
  [request token]
  [map? (? string?) :ret map?]
  (if token
    (mock/header request "Authorization" (format "Token %s" token))
    request))

(>defn add-csrf-header
  "Adds the authorization header to a request map."
  [request]
  [map? :ret map?]
  (mock/header request "x-schnaq-csrf" "this content does not matter"))

(>defn accept-edn-response-header
  "Add header to accept edn as our valid response type."
  [request]
  [map? :ret map?]
  (mock/header request "Accept" "application/edn"))

;; -----------------------------------------------------------------------------

(def test-app
  "Initialize a test-application.
  This can be used to make real requests to the api, but without
  side-effect-services, like the websockets."
  (api/app))

;; -----------------------------------------------------------------------------
;; Files

(def image
  {:content "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABAQMAAAAl21bKAAAAA1BMVEUAAACnej3aAAAAAXRSTlMAQObYZgAAAApJREFUCNdjYAAAAAIAAeIhvDMAAAAASUVORK5CYII=",
   :name "1x1.png",
   :type "image/png"})

(def file
  {:name "file.txt"
   :size 32
   :type "text/plain"
   :content "data:application/octet-stream;base64,V2lsbGtvbW1lbiBpbSBzY2huYXFxaXBhcmFkaWVzIQo="})
