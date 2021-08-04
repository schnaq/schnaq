(ns schnaq.auth
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.core.keys :as keys]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [unauthorized forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.shared :as shared-config]))

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
  (if shared-config/production?
    (wrap-authentication handler signed-jwt-backend)
    (wrap-authentication handler signed-jwt-backend signed-jwt-backend-for-testing)))

(defn replace-bearer-with-token
  "Most tools send the an authorization header as \"Bearer <token>\", but buddy
  wants it as \"Token <token>\". This middleware transforms the request, if a
  JWT is sent in the header."
  [handler]
  (fn [request]
    (if-let [bearer-token (get-in request [:headers "authorization"])]
      (let [[_bearer token] (string/split bearer-token #" ")]
        (handler (assoc-in request [:headers "authorization"] (format "Token %s" token))))
      (handler request))))


;; -----------------------------------------------------------------------------

(>defn has-role?
  "Check if user has realm-wide admin access."
  [request roles]
  [map? coll? :ret boolean?]
  (string? (some roles
                 (get-in request [:identity :roles]))))

(defn- valid-app-code?
  "Check if an app-code was provided via the request-body."
  [request]
  (string? (some config/app-codes
                 #{(get-in request [:parameters :body :app-code])})))

(defn authenticated?-middleware
  "Validate, that user is logged-in."
  [handler]
  (fn [request]
    (if (authenticated? request)
      (-> request
          (assoc-in [:identity :id] (get-in request [:identity :sub]))
          (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))
          (assoc-in [:identity :admin?] (has-role? request shared-config/admin-roles))
          handler)
      (unauthorized (at/build-error-body :auth/not-logged-in
                                         "You are not logged in. Maybe your token is malformed / expired.")))))

(defn admin?-middleware
  "Check if user has admin-role."
  [handler]
  (fn [request]
    (if (has-role? request shared-config/admin-roles)
      (handler request)
      (forbidden (at/build-error-body :auth/not-an-admin "You are not an admin.")))))

(defn beta-tester?-middleware
  "Check if is eligible for our beta-testers program."
  [handler]
  (fn [request]
    (if (has-role? request shared-config/beta-tester-roles)
      (handler request)
      (forbidden (at/build-error-body :auth/not-a-beta-tester "You are not a beta tester.")))))

(defn valid-app-code?-middleware
  "Validate the app code provided by the application. Only registered
  microservices should be allowed to post data to our servers."
  [handler]
  (fn [request]
    (if (valid-app-code? request)
      (handler request)
      (forbidden (at/build-error-body :app/invalid-code "Your application has no permission to access this API. Please provide a valid app-code in your request.")))))

(>defn member-of-group?
  "Check if group is available in the JWT token."
  [identity group]
  [map? string? :ret boolean?]
  (some #(= group %) (:groups identity)))
