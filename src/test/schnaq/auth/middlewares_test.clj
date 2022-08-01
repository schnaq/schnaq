(ns schnaq.auth.middlewares-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [compojure.core :refer [routes GET wrap-routes]]
            [ring.mock.request :as mock]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.auth.middlewares :as auth-middlewares]
            [schnaq.database.user :as user-db]
            [schnaq.test-data :refer [schnaqqi alex kangaroo]]
            [schnaq.test.toolbelt :as schnaq-toolbelt :refer [token-schnaqqifant-user token-n2o-admin token-wrong-signature token-timed-out mock-authorization-header]]))

(use-fixtures :each schnaq-toolbelt/init-test-delete-db-fixture)
(use-fixtures :once schnaq-toolbelt/clean-database-fixture)

(def ^:private alex-keycloak-id (:user.registered/keycloak-id alex))
(def ^:private schnaqqi-keycloak-id (:user.registered/keycloak-id schnaqqi))
(def ^:private kangaroo-keycloak-id (:user.registered/keycloak-id kangaroo))

(def ^:private test-routes
  "Define own routes just for testing."
  (routes
   (-> (GET "/test/admin/authentication" []
         (fn [request] (ok (get-in request [:identity :preferred_username]))))
       (wrap-routes auth-middlewares/admin?-middleware)
       (wrap-routes auth-middlewares/authenticated?-middleware)
       (wrap-routes auth-middlewares/update-jwt-middleware)
       (wrap-routes auth/wrap-jwt-authentication))
   (-> (GET "/test/user/authentication" []
         (fn [request] (ok (get-in request [:identity :preferred_username]))))
       (wrap-routes auth-middlewares/authenticated?-middleware)
       (wrap-routes auth-middlewares/update-jwt-middleware)
       (wrap-routes auth/wrap-jwt-authentication))))

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

(deftest pro-user?-middleware-test
  (let [alex (user-db/update-user {:user.registered/keycloak-id alex-keycloak-id
                                   :user.registered/roles :role/pro})
        mw (auth-middlewares/pro-user?-middleware (constantly :success))]
    (testing "Non-existent user is no pro user."
      (is (= 403 (:status (mw {:identity {:sub "non-existent-user"}})))))
    (testing "Normal registered users have no access."
      (is (= 403 (:status (mw {:user (user-db/private-user-by-keycloak-id kangaroo-keycloak-id)})))))
    (testing "Pro-User shall pass."
      (is (= :success (mw {:user alex}))))
    (testing "Beta-Users also have access to pro-features."
      (is (= :success (mw {:user (user-db/private-user-by-keycloak-id schnaqqi-keycloak-id)}))))))
