(ns schnaq.auth-test
  (:require [buddy.core.keys :as keys]
            [clojure.test :refer [deftest testing is are]]
            [schnaq.auth :as sut]
            [clojure.string :as string]))

(def ^:private valid-public-key
  "Public RSA key of keycloak client."
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtsaglvgpZHk0TJIXMuCIcm6xfvC9MgXCDaK8aCigBWd+qBOLPJsULHlbmlT74f6mchBPjIxxedqGt/MjUI0leJm/1W0l+BBEfpp2FC9VQE40cRL+M4qE7rdj3JEogr+x7tz912hvPmvw90Vo3H57OMklv3R8QFzFirBlRO8TcsXJgaYFBPCezpLhEMwKKy1LkxvzkrF0STbRysXk+yzIPRYsBYhdegYMyL3D36CwFUolysyuepHC+DcNsp8wrSk9DrH4RSIfUsk2Kn7IEfcY+d65DNOzzQ0M4mAvHxrieSk52KT4HD70NwVqkZxsGbvDFQ4dHPdqXiO+hb3Zux3EZwIDAQAB\n-----END PUBLIC KEY-----"))

(def ^:private wrong-public-key
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl9JrfD8moySWXe0G1lFeK2w376n6HzUXDwcnLR5XapQIOr5XyZVo35QRzoJnp5oN4Im/sO5K2VZh+9lBY6bdBaCjcMtTFFd1SF30hIJGMlZOXLC9qy6odIPjtwhNkzl8LqDfLzAW8eo6IS+ezMmNq2MJtsYcz1hhI8LmE+DHXdQ+gYqipRf7WyUUORicuTHaPdJOPKCk6O3FuvGqWUyO37leToho7MY/rTfllc/Sbxjxg8PX1nxTK/9KGU+svRfhMeYkyD2KJBOPQFHh1pHZFwv8TyDebKxml4l3NRNQWe4GcBrs0o8OPTNDJamknJDKFo5y3oM2YEzoS1YVNNpGJQIDAQAB\n-----END PUBLIC KEY-----"))

(def ^:private christian-admin-token
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4MzkyMTksImlhdCI6MTYxMTgzODkxOSwiYXV0aF90aW1lIjoxNjExODMxNzI2LCJqdGkiOiJiMzA3MzNiOS00NjExLTRjOGMtYjY3OS1mZmJmNDUwNDdkOTIiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImJjYWVjMDgyLTI0YTgtNDU4Mi1hY2UxLTg2NjUyOTE1YTIyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiJiMjc4MmUyYS1kMzQwLTRhZGUtOGQ0OS03N2ZhMzUxNWMzNWQiLCJzZXNzaW9uX3N0YXRlIjoiZTg1Njk4YzItYzMzMy00YjUwLWI3ZmItNDU1YWQ1NjM0OGE0IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RpYW4gTWV0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuMm8iLCJnaXZlbl9uYW1lIjoiQ2hyaXN0aWFuIiwiZmFtaWx5X25hbWUiOiJNZXRlciIsImVtYWlsIjoiY21ldGVyQGdvb2dsZW1haWwuY29tIn0.F8Pepi93FqBB6P3la7KHqFFhZ3k4EzHC2Nz-s6x8PwoyZnhgEqAsEmklNeXY4O45U2tYRVkcfTtMAo74LVa8VacRbaUoIXTq5Q3VlQNGJW35oSFNMPOSGKTfj216rj62J9utyfmF-l1bIV803xon2lQXzVAcQ3JVTbnd3T-PcpPh3YlwwiAv1kmQ-zJ6znXOFkhSmT0BCarvWyQRctQa2rNsCsKgX8LjtSnrVPeb8pxuMOYrQPuOHAJ4jtYZ4EcLU-Z7GZdG367jtQ263pR-hfxOONwy47ihRNwcZJAtjxL7emUv9UozKzldWhudliC8J2OBin-xbi460JJkE0RaFA")

(def ^:private schnaqqifant-user-token
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4NTE4MzAsImlhdCI6MTYxMTg1MTUzMCwiYXV0aF90aW1lIjoxNjExODUxNTI1LCJqdGkiOiJiOTA3ZTJjMi0wM2FmLTQwOTAtOGI0MS0wOGFkOGIzZGUwYTYiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZmODcwOTRjLTRhNjctNDdlOC04NTQzLTQ2ZTAwNzRkYjQ5MiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIzNjIwN2IyMS05NTY2LTRiYTItODE4NC0xOTJmMGZhZWE5MGQiLCJzZXNzaW9uX3N0YXRlIjoiMjJmNDJhYmMtMWEzMy00ODliLWIyYzktODI3YWE2M2U3YjY2IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJzY2huYXFxaSBmYW50IiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2NobmFxcWlmYW50IiwiZ2l2ZW5fbmFtZSI6InNjaG5hcXFpIiwiZmFtaWx5X25hbWUiOiJmYW50IiwiZW1haWwiOiJpbmZvQHNjaG5hcS5jb20ifQ.lupfFWowuI3TrkRsaR782R6VG6ZwRFyIoRzMCD0AXw_Xb0zoqFvaJZGoMJhzeiu5s-U2gCU8YF3UwMn3sn16oJqnGtEPWKYX_CFvYd59N5MluXJaxGtzGORsR8LfTRUKeC_RkaOe_qPBqScOsvy0gHJCtUGUlAm50vbkK6eer8GwhpclUcHaOfrhEiP5U6yzJsW-dY7rKPojwfsV_KXPFK-1BtJn_yH_Fuv7jkUcPBx86zX_iVIaZfXjo3Y24kYZuVmx6euWs2yOoSXU9RxRpTxJtBIdHC2vwmizivPdY2KGbah9CmfFqyM45kg4lY22qLssrFvlbgG-ifkLukcnTw")

(def ^:private long-lasting-token
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4ODM1NDcsImlhdCI6MTYxMTg0NzU1MSwiYXV0aF90aW1lIjoxNjExODQ3NTQ3LCJqdGkiOiI4YmVlODBlOS0zNmE5LTRjYWMtOTdkMi0zMGVhYWViOWQ3Y2QiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImJjYWVjMDgyLTI0YTgtNDU4Mi1hY2UxLTg2NjUyOTE1YTIyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIxZjRhNTJlMS1kYTcyLTQ1OTctYjU5My01M2Q0ZDBhZDY2YmEiLCJzZXNzaW9uX3N0YXRlIjoiMTViMmFkMjgtMDliYi00NzEyLTgzZGItNjZiNGM3MmRhZTI5IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RpYW4gTWV0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuMm8iLCJnaXZlbl9uYW1lIjoiQ2hyaXN0aWFuIiwiZmFtaWx5X25hbWUiOiJNZXRlciIsImVtYWlsIjoiY21ldGVyQGdvb2dsZW1haWwuY29tIn0.CBTVqxqrgYHHHdPsc0YgBaaltnI5kcIYOKl6L0L5svZiylJb7AvIejk05GyDfpZUzUn_5eykvBylGcVkH4VZrtR8mKvGXj7l4sncvqUqFFpAK2Wd7jBqorQAlSi9muQIWtgWGb1Da_x4tQ5OOb_EulmqNsE8xFXuvzsneci-H7DUWscsGiLNndaOnZCMcIq6dn49zzj9u1qKMKP5Wgz77CiCPSailOfU-Zo5MAkVcPlK6GuFRSdeyUmmYV3uJcKq9v319sDueP3tvLiTeUdCB0hVShuNY_6CVmkjh_oWev9dv2un7UU7N-4NX6-GLrCo83g-VsQ8SwfmnnovD93VNQ")

(def ^:private request-with-admin-token
  {:headers {"authorization" (format "Bearer %s" christian-admin-token)}})

(def ^:private request-with-user-token
  {:headers {"authorization" (format "Bearer %s" schnaqqifant-user-token)}})

(def ^:private request-no-authorization-token
  {:headers {}})

;; -----------------------------------------------------------------------------

(deftest valid-token?-test
  (let [valid-token? #'sut/valid-token?]
    (testing "Valid old token with correct public key is well formed, but expired."
      (is (string/includes? (valid-token? christian-admin-token valid-public-key)
                            "expired"))
      (is (string/includes? (valid-token? schnaqqifant-user-token valid-public-key)
                            "expired")))
    (testing "Valid old token with wrong public key is malformed (signature
    can't get verified)."
      (is (string/includes? (valid-token? christian-admin-token wrong-public-key)
                            "corrupt or manipulated"))
      (is (string/includes? (valid-token? schnaqqifant-user-token wrong-public-key)
                            "corrupt or manipulated")))))

(deftest decode-token-test
  (let [decode-token #'sut/decode-token]
    (testing "Sample token should correctly be decoded."
      (is (string? (get-in (decode-token christian-admin-token) [:token/header "typ"])))
      (is (string? (get-in (decode-token schnaqqifant-user-token) [:token/header "typ"]))))))

(deftest has-admin-role?-test
  (let [has-admin-role? #'sut/has-admin-role?]
    (testing "Christian has admin role, schnaqqifant not."
      (is (has-admin-role? christian-admin-token))
      (is (not (has-admin-role? schnaqqifant-user-token))))))

(deftest token-present?-test
  (let [has-token? #'sut/has-token?]
    (testing "Check if the authorization token is present in the header."
      (is (has-token? request-with-admin-token))
      (is (has-token? request-with-user-token))
      (is (not (has-token? request-no-authorization-token)))
      (is (not (has-token? {}))))))

(deftest with-valid-token-test
  (let [with-valid-token' (partial sut/with-valid-token identity)]
    (testing "Request with token should contain a valid token."
      (is (= request-with-admin-token
             (with-valid-token' request-with-admin-token)))
      (is (= request-with-user-token
             (with-valid-token' request-with-user-token))))
    (testing "Missing token should lead to an error."
      (is (= 400 (:status (with-valid-token' request-no-authorization-token))))
      (is (= 400 (:status (with-valid-token' {})))))))

(deftest with-admin-access-test
  (let [with-admin-access' (partial sut/with-admin-access identity)]
    (testing "Requests with valid token and admin access should be fine."
      (is (= request-with-admin-token
             (with-admin-access' request-with-admin-token))))
    (testing "Requests with valid token, but no admin access, should result in a
    forbidden response"
      (is (= 401 (:status (with-admin-access' request-with-user-token)))))
    (testing "Invalid token should still result in a bad request response."
      (is (= 400 (:status (with-admin-access' request-no-authorization-token))))
      (is (= 400 (:status (with-admin-access' {})))))))


