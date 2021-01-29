(ns schnaq.auth
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [ghostwheel.core :refer [>defn >defn-]]
            [ring.util.http-response :refer [ok bad-request unauthorized]]
            [schnaq.config :as config]))

(def signed-jwt-backend (backends/jws {:secret config/keycloak-public-key
                                       :options {:alg :rs256}}))

(>defn- has-admin-role?
  "Check if user has realm-wide admin access."
  [request]
  [map? :ret boolean?]
  (= "admin" (some #{"admin"}
                   (get-in request [:identity :realm_access :roles]))))

(defn wrap-jwt-authentication
  "Use buddys jwt backend with our public key for authentication."
  [handler]
  (wrap-authentication handler signed-jwt-backend))

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
