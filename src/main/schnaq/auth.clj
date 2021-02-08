(ns schnaq.auth
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.core.keys :as keys]
            [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [unauthorized]]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.core :as schnaq-core]))

(def ^:private public-key-for-test-backend
  "Public key just for testing purposes."
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4icNzKKV/FhHFmYTRZGp28Gc7Vk4TAsCRsdb3w2Mv7EUzAOgdB0/I+VluvIN7u9wwohdg+UThjtQYY4tW036AefBgG6qdjpnGkxovmx1tlPEloR0fcnFUyB19XCQVhWOktnvQ45MB/O7VcxK14+A0t16iBcULT91mnqeKm08WiFgdU7s2qONPfDsMGOH7XBX4RNiBshxEqlpmAMvyxDHitqzt2bLJ9E08OCNbFKjXCMc763E/t90/RqEVGQsMGAu49Pi12r2ZszJYwZRMNYl0EJIfPeX4g9q0BdcuqKGvnjwyllFOswD/NE5htnLhJUP8aNS+45iHysMwyy7AuhvEwIDAQAB\n-----END PUBLIC KEY-----"))

(def ^:private signed-jwt-backend-for-testing
  "Second backend to validate the test-tokens."
  (backends/jws {:secret public-key-for-test-backend
                 :options {:alg :rs256}}))

(def ^:private signed-jwt-backend
  "Primary backend for JWT validation."
  (backends/jws {:secret keycloak-config/keycloak-public-key
                 :options {:alg :rs256}}))

(defn wrap-jwt-authentication
  "Use buddys jwt backend with our public key for authentication."
  [handler]
  (if schnaq-core/production-mode?
    (wrap-authentication handler signed-jwt-backend)
    (wrap-authentication handler signed-jwt-backend signed-jwt-backend-for-testing)))


;; -----------------------------------------------------------------------------

(>defn has-admin-role?
  "Check if user has realm-wide admin access."
  [request]
  [map? :ret boolean?]
  (= "admin" (some #{"admin"}
                   (get-in request [:identity :roles]))))

(defn auth-middleware
  "Validate, that user is logged-in."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (-> request
          (assoc-in [:identity :id] (get-in request [:identity :sub]))
          (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))
          (assoc-in [:identity :admin?] (has-admin-role? request))
          handler)
      (unauthorized "You are not logged in. Maybe your token is malformed / expired."))))

(defn is-admin-middleware
  "Check if user has admin-role."
  [handler]
  (fn [request]
    (if (has-admin-role? request)
      (handler request)
      (unauthorized "You are not an admin."))))
