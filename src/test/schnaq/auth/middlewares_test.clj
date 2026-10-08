(ns schnaq.auth.middlewares-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [reitit.ring :as ring]
            [ring.mock.request :as mock]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.auth.middlewares :as auth-middlewares]
            [schnaq.test.toolbelt :as schnaq-toolbelt :refer [token-schnaqqifant-user token-n2o-admin token-wrong-signature token-timed-out mock-authorization-header]]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(defn- identity-handler
  "Return the JWT-parsed preferred_username for the authenticated request."
  [request]
  (ok (get-in request [:identity :preferred_username])))

(def ^:private test-routes
  "Reitit-based handler exercising the auth middleware stack."
  (ring/ring-handler
   (ring/router
    [["/test/admin/authentication"
      {:get {:middleware [auth/wrap-jwt-authentication
                          auth-middlewares/parse-jwt-middleware
                          auth-middlewares/authenticated?-middleware
                          auth-middlewares/admin?-middleware]
             :handler identity-handler}}]
     ["/test/user/authentication"
      {:get {:middleware [auth/wrap-jwt-authentication
                          auth-middlewares/parse-jwt-middleware
                          auth-middlewares/authenticated?-middleware]
             :handler identity-handler}}]])))

(deftest valid-jwt-in-header-test
  (let [path "/test/user/authentication"
        response #(test-routes (-> (mock/request :get path)
                                   (mock-authorization-header %)))]
    (testing "Users must provide a valid JWT token in the request headers to
  access this route."
      (is (= "schnaqqi" (:body (response token-schnaqqifant-user))))
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
      (is (= 403 (:status (response token-schnaqqifant-user)))))
    (testing "Wrong, old or missing tokens have no access."
      (is (= 401 (:status (response token-timed-out))))
      (is (= 401 (:status (response token-wrong-signature))))
      (is (= 401 (:status (test-routes (mock/request :get path))))))))

(deftest parse-jwt-registers-unknown-users-test
  (let [handler (-> (fn [request] (ok (select-keys request [:user :new-user?])))
                    auth-middlewares/parse-jwt-middleware
                    auth/wrap-jwt-authentication)
        token (schnaq-toolbelt/token-for-new-user "0b6f2a54-3c8e-4d71-9f0a-7e2d1c4b5a66")
        response #(:body (handler (mock-authorization-header (mock/request :get "/") token)))]
    (testing "A logged-in user who is not in our database is registered on the first request."
      (let [{:keys [user new-user?]} (response)]
        (is new-user?)
        (is (= "0b6f2a54-3c8e-4d71-9f0a-7e2d1c4b5a66" (:user.registered/keycloak-id user)))))
    (testing "Later requests find the registered user."
      (let [{:keys [user new-user?]} (response)]
        (is (nil? new-user?))
        (is (some? user))))))
