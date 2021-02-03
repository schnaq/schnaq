(ns schnaq.auth-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [routes GET wrap-routes]]
            [ring.mock.request :as mock]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]))

;; Tokens are valid until the year 2200
(def ^:private token-n2o-admin
  "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImVMTzhUR1hoc296akFuV2FtSG5UUVc2RTNSOElYUEFOaGJ3MDV6d1BwYVEifQ.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTYxMTkyMzcwMiwiYXV0aF90aW1lIjoxNjExOTIzNjk5LCJqdGkiOiI3NDhhZTc5Yy0zZDIyLTQ5MDctYWFmNi1hMTE3ZTVhMWEwMmUiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImJjYWVjMDgyLTI0YTgtNDU4Mi1hY2UxLTg2NjUyOTE1YTIyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIzZGNmN2E4My05OTczLTRhY2UtOGQ1Yy0yNjhkMTY3M2MzZjgiLCJzZXNzaW9uX3N0YXRlIjoiOTZjZTA5YmMtYTc4Zi00NjJiLWJmZmEtYTI4MjQzNzdjZmVhIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RpYW4gTWV0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuMm8iLCJnaXZlbl9uYW1lIjoiQ2hyaXN0aWFuIiwiZmFtaWx5X25hbWUiOiJNZXRlciIsImVtYWlsIjoiY21ldGVyQGdvb2dsZW1haWwuY29tIn0.q2udRxZVBJ_9WHGP-FIvJptD0G2mQO2AsFi5FhODcYoOPUCTGbuAKNNKoRJyOOUi2P7SbJinColVEU-1iub0uANloUhtsM5JC6m_1yk5rnYn_OKZt0w9vaKgl5suqmv2-spJQ0ZHTkgWIIenZFQ5XpW036XTsDSfLrqphFtYn4XpgOSRhZgjf0dkf9s45HmZUK3Z9cFQr-eknNxBCWbM31S8rb-1lZk8HXNftsp-9rc49kftVS83EKfYW6JGkbRifEEo1obdxWVasdPUpd3Wb71-WszuS35qvmvSa3AgdMBolw7iDY4zt5lBCgREv5TIUBVP_uMLo3rWgoM9lgGeHA")
(def ^:private token-schnaqqifant-user
  "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImVMTzhUR1hoc296akFuV2FtSG5UUVc2RTNSOElYUEFOaGJ3MDV6d1BwYVEifQ.eyJleHAiOjcyNTgxMTg0MDAsImlhdCI6MTYxMTkyNDYyNSwiYXV0aF90aW1lIjoxNjExOTI0NjIyLCJqdGkiOiIyZjQ0ZjYwZS01MGM5LTRmNWEtOTgwZi0zZjM1ZmI2ODQ3YWUiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZmODcwOTRjLTRhNjctNDdlOC04NTQzLTQ2ZTAwNzRkYjQ5MiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIxMGE5YTgzZC00MWI1LTQxNDMtYWIyNS01MDM1M2YyMWI2ZWEiLCJzZXNzaW9uX3N0YXRlIjoiZGMxMDM2ZDctNjlmNS00OGJiLThiZjQtYTRlOTdlYTQyYzExIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJzY2huYXFxaSBmYW50IiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2NobmFxcWlmYW50IiwiZ2l2ZW5fbmFtZSI6InNjaG5hcXFpIiwiZmFtaWx5X25hbWUiOiJmYW50IiwiZW1haWwiOiJpbmZvQHNjaG5hcS5jb20ifQ.pkWA-KbPIqtWVnCSL_sKRc1_F-Q-iKUixnidMrr66_1E2knpxm-0QBPtc-S0ehjAI9HPNjnhpIG3p9mVxekS9zQKpwThqWxfDNinz4SNWpWUpyDEwDK6IFfpRr70crUT-MlLfevI9vfXUh-C6PMSKEL0dSkLXVkOC7TOBSNHVQdtm2P34UHMexUcHGpGxQkGpKR_ubRHNa42uZjjRBaM-dr8RMxT2e9zoTh_J3-cXBgY_YEsLxvX3FlRE2kKPWvHy8u_ve5340Fkj8RtexVNlE_bSc035VZRXMhbw2RbXkEv15cRdwh6K7kKMcHzTj0JhcoE8V7PVJnqaQtBGBVkNA")
(def ^:private token-wrong-signature
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJlTE84VEdYaHNvempBbldhbUhuVFFXNkUzUjhJWFBBTmhidzA1endQcGFRIn0.eyJleHAiOjE2MTE4NTE4MzAsImlhdCI6MTYxMTg1MTUzMCwiYXV0aF90aW1lIjoxNjExODUxNTI1LCJqdGkiOiJiOTA3ZTJjMi0wM2FmLTQwOTAtOGI0MS0wOGFkOGIzZGUwYTYiLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImZmODcwOTRjLTRhNjctNDdlOC04NTQzLTQ2ZTAwNzRkYjQ5MiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiIzNjIwN2IyMS05NTY2LTRiYTItODE4NC0xOTJmMGZhZWE5MGQiLCJzZXNzaW9uX3N0YXRlIjoiMjJmNDJhYmMtMWEzMy00ODliLWIyYzktODI3YWE2M2U3YjY2IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsIm5hbWUiOiJzY2huYXFxaSBmYW50IiwicHJlZmVycmVkX3VzZXJuYW1lIjoic2NobmFxcWlmYW50IiwiZ2l2ZW5fbmFtZSI6InNjaG5hcXFpIiwiZmFtaWx5X25hbWUiOiJmYW50IiwiZW1haWwiOiJpbmZvQHNjaG5hcS5jb20ifQ.lupfFWowuI3TrkRsaR782R6VG6ZwRFyIoRzMCD0AXw_Xb0zoqFvaJZGoMJhzeiu5s-U2gCU8YF3UwMn3sn16oJqnGtEPWKYX_CFvYd59N5MluXJaxGtzGORsR8LfTRUKeC_RkaOe_qPBqScOsvy0gHJCtUGUlAm50vbkK6eer8GwhpclUcHaOfrhEiP5U6yzJsW-dY7rKPojwfsV_KXPFK-1BtJn_yH_Fuv7jkUcPBx86zX_iVIaZfXjo3Y24kYZuVmx6euWs2yOoSXU9RxRpTxJtBIdHC2vwmizivPdY2KGbah9CmfFqyM45kg4lY22qLssrFvlbgG-ifkLukcnTw")
(def ^:private token-timed-out
  "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJQV1pHYnRvc21lUjhGb1dwWTJzSUhEUy14Q1ZlR3dxRXFSQU80NVRBcUFFIn0.eyJleHAiOjE2MTE5MzA5NjQsImlhdCI6MTYxMTkzMDY2NCwiYXV0aF90aW1lIjoxNjExOTMwMTIwLCJqdGkiOiJlY2U0Mjc5My0xOTBiLTQ5NzktYWUxMy01NGYwYzJjOWM0OTciLCJpc3MiOiJodHRwczovL2tleWNsb2FrLmRpc3F0ZWMuY29tL2F1dGgvcmVhbG1zL2RldmVsb3BtZW50IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImJjYWVjMDgyLTI0YTgtNDU4Mi1hY2UxLTg2NjUyOTE1YTIyZiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImRldmVsb3BtZW50Iiwibm9uY2UiOiI1Zjk2YjNhNi01MGE5LTRhMDEtODAzNi0zMTIzZjQ0YzczMDIiLCJzZXNzaW9uX3N0YXRlIjoiYTBkM2QzY2EtNzc3Yy00YjY5LWJlNTYtYTA1ZTNhN2UwYzFlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyJodHRwOi8vbG9jYWxob3N0Ojg3MDAiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJDaHJpc3RpYW4gTWV0ZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJuMm8iLCJnaXZlbl9uYW1lIjoiQ2hyaXN0aWFuIiwiZmFtaWx5X25hbWUiOiJNZXRlciIsImVtYWlsIjoiY21ldGVyQGdvb2dsZW1haWwuY29tIn0.ox_GqeEqFTS_WOYZvVYfBvXuqFk5KY3DbJlVTF_TZIbU4JvAretHJ6OYV2D3nKezi41Mk0BuTRb13gy46IMXHaacSKNdBsBOC1XcVNUPsONXfKSBkUZR-qX7Z4BZ2dEwWTTtYFjoenyHMk_OTTuBTQ89thUhZzyYJGjaOsNl8YWVleMbV_FBqbdiw_mIM60AAz94_u_qwmgJ2BU_SCUE1J_nrG0aHMGdzN7J-Z10SkvR8vKEWmdNL4UN9z4MDjIg3khj2gMC7M5-VpeMXkgcQp-_l7hWkcTkLgpRvVbdmh2LsBcuA0vUjSOusuJSLnAiwjqB1dn6LhNo7Dry892JTw")

;; -----------------------------------------------------------------------------

(defn- mock-authorization-header [request token]
  (mock/header request "Authorization" (format "Token %s" token)))

(def ^:private test-routes
  "Define own routes just for testing."
  (routes
    (-> (GET "/test/admin/authentication" []
          (fn [request] (ok (get-in request [:identity :preferred_username]))))
        (wrap-routes auth/is-admin-middleware)
        (wrap-routes auth/auth-middleware)
        (wrap-routes auth/wrap-jwt-authentication))
    (-> (GET "/test/user/authentication" []
          (fn [request] (ok (get-in request [:identity :preferred_username]))))
        (wrap-routes auth/auth-middleware)
        (wrap-routes auth/wrap-jwt-authentication))))

(deftest valid-jwt-in-header-test
  (let [path "/test/user/authentication"
        response #(test-routes (-> (mock/request :get path)
                                   (mock-authorization-header %)))]
    (testing "Users must provide a valid JWT token in the request headers to
  access this route."
      (is (= "schnaqqifant" (:body (response token-schnaqqifant-user))))
      (is (= "n2o" (:body (response token-n2o-admin)))))
    (testing "Wrong tokens shall not pass."
      (is (= 401 (:status (response token-wrong-signature))))
      (is (= 401 (:status (response token-timed-out)))))
    (testing "Missing token shall also not pass."
      (is (= 401 (:status (test-routes (mock/request :get path))))))))

(deftest admin-middleware-test
  (let [path "/test/admin/authentication"
        response #(test-routes (-> (mock/request :get path)
                                   (mock-authorization-header %)))]
    (testing "JWT token with admin role shall pass."
      (is (= "n2o" (:body (response token-n2o-admin)))))
    (testing "Valid JWT, but no admin role, has no access."
      (is (= 401 (:status (response token-schnaqqifant-user)))))
    (testing "Wrong, old or missing tokens have no access."
      (is (= 401 (:status (response token-timed-out))))
      (is (= 401 (:status (response token-wrong-signature))))
      (is (= 401 (:status (test-routes (mock/request :get path))))))))