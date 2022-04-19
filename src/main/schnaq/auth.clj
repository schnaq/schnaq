(ns schnaq.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [com.fulcrologic.guardrails.core :refer [>defn]]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.shared :as shared-config]))

(defn- build-signed-jwt-backend
  "Takes a converted public key and builds a signed jwt backend to be wrapped
  by the handler."
  [public-key]
  (when public-key
    (backends/jws {:secret public-key
                   :options {:alg :rs256}})))

(def ^:private signed-jwt-backend-for-testing
  "Second backend to validate the test-tokens."
  (build-signed-jwt-backend config/testing-public-key))

(def ^:private signed-jwt-backend
  "Primary backend for JWT validation."
  (build-signed-jwt-backend keycloak-config/keycloak-public-key))

(defn wrap-jwt-authentication
  "Middleware to validate the JWT tokens. Multiple backends are supported."
  [handler]
  (let [backend  [signed-jwt-backend]]
    (if shared-config/production?
      (apply wrap-authentication handler backend)
      (apply wrap-authentication handler (conj backend signed-jwt-backend-for-testing)))))

(>defn member-of-group?
  "Check if group is available in the JWT token."
  [identity group]
  [map? string? :ret boolean?]
  (some #(= group %) (:groups identity)))
