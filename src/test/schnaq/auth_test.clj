(ns schnaq.auth-test
  (:require [clojure.test :refer [deftest testing is]]
            [compojure.core :refer [routes GET wrap-routes]]
            [ring.mock.request :as mock]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.test.toolbelt :refer [token-schnaqqifant-user token-n2o-admin token-wrong-signature token-timed-out mock-authorization-header]]))

(def ^:private test-routes
  "Define own routes just for testing."
  (routes
    (-> (GET "/test/admin/authentication" []
          (fn [request] (ok (get-in request [:identity :preferred_username]))))
        (wrap-routes auth/admin?-middleware)
        (wrap-routes auth/authenticated?-middleware)
        (wrap-routes auth/wrap-jwt-authentication))
    (-> (GET "/test/user/authentication" []
          (fn [request] (ok (get-in request [:identity :preferred_username]))))
        (wrap-routes auth/authenticated?-middleware)
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
      (is (= 403 (:status (response token-schnaqqifant-user)))))
    (testing "Wrong, old or missing tokens have no access."
      (is (= 401 (:status (response token-timed-out))))
      (is (= 401 (:status (response token-wrong-signature))))
      (is (= 401 (:status (test-routes (mock/request :get path))))))))

(deftest member-of-group?-test
  (testing "Verify that user is member of called group."
    (let [identity (:identity (assoc-in (mock/request :get "/testing/stuff")
                                        [:identity :groups] ["these-are-my-groups" "schnaqqifantenparty"]))]
      (is (auth/member-of-group? identity "schnaqqifantenparty"))
      (is (not (auth/member-of-group? identity "")))
      (is (not (auth/member-of-group? identity "not-member-of"))))))
