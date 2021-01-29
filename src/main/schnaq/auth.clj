(ns schnaq.auth
  (:require [buddy.auth :refer [authenticated?]]
            [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [ghostwheel.core :refer [>defn >defn-]]
            [ring.util.http-response :refer [ok bad-request unauthorized]]
            [schnaq.config :as config]
            [clojure.spec.alpha :as s]
            [buddy.core.keys :as keys]))

(s/def :token/payload map?)
(s/def :token/header map?)
(def ^:private wrong-public-key
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl9JrfD8moySWXe0G1lFeK2w376n6HzUXDwcnLR5XapQIOr5XyZVo35QRzoJnp5oN4Im/sO5K2VZh+9lBY6bdBaCjcMtTFFd1SF30hIJGMlZOXLC9qy6odIPjtwhNkzl8LqDfLzAW8eo6IS+ezMmNq2MJtsYcz1hhI8LmE+DHXdQ+gYqipRf7WyUUORicuTHaPdJOPKCk6O3FuvGqWUyO37leToho7MY/rTfllc/Sbxjxg8PX1nxTK/9KGU+svRfhMeYkyD2KJBOPQFHh1pHZFwv8TyDebKxml4l3NRNQWe4GcBrs0o8OPTNDJamknJDKFo5y3oM2YEzoS1YVNNpGJQIDAQAB\n-----END PUBLIC KEY-----"))

(def signed-jwt-backend (backends/jws {:secret config/keycloak-public-key
                                       :options {:alg :rs256}}))

(defn wrap-jwt-authentication
  [handler]
  (wrap-authentication handler signed-jwt-backend))

(defn auth-middleware
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :body {:error "Unauthorized"}})))

(defn testview
  "testing"
  [request]
  (prn request)
  (def foo request)
  (ok {:message "Jeaasdqweh"}))
