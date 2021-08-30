(ns schnaq.auth
  (:require [buddy.auth.backends :as backends]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.core.keys :as keys]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config.keycloak :as keycloak-config]
            [schnaq.config.shared :as shared-config]
            [taoensso.timbre :as log]))

(def jwt-public-key (atom nil))

(defn load-jwt-public-key
  "Load a public jwt key if provided by the environment. Makes it possible to
  add an additional jwt-backend, e.g. if embedded in a site which takes care
  of the user accounts."
  ([]
   (when-let [jwt-public-key-url (System/getenv "JWT_PUBLIC_KEY_URL")]
     (load-jwt-public-key jwt-public-key-url)))
  ([jwt-public-key-url]
   (log/info (format "Loading public JWT key from %s" jwt-public-key-url))
   (let [public-key (slurp jwt-public-key-url)]
     (reset! jwt-public-key (keys/str->public-key public-key)))))

(def ^:private public-key-for-test-backend
  "Public key just for testing purposes."
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4icNzKKV/FhHFmYTRZGp28Gc7Vk4TAsCRsdb3w2Mv7EUzAOgdB0/I+VluvIN7u9wwohdg+UThjtQYY4tW036AefBgG6qdjpnGkxovmx1tlPEloR0fcnFUyB19XCQVhWOktnvQ45MB/O7VcxK14+A0t16iBcULT91mnqeKm08WiFgdU7s2qONPfDsMGOH7XBX4RNiBshxEqlpmAMvyxDHitqzt2bLJ9E08OCNbFKjXCMc763E/t90/RqEVGQsMGAu49Pi12r2ZszJYwZRMNYl0EJIfPeX4g9q0BdcuqKGvnjwyllFOswD/NE5htnLhJUP8aNS+45iHysMwyy7AuhvEwIDAQAB\n-----END PUBLIC KEY-----"))

(defn- build-signed-jwt-backend
  "Takes a converted public key and builds a signed jwt backend to be wrapped
  by the handler."
  [public-key]
  (when public-key
    (backends/jws {:secret public-key
                   :options {:alg :rs256}})))

(def ^:private signed-jwt-backend-for-testing
  "Second backend to validate the test-tokens."
  (build-signed-jwt-backend public-key-for-test-backend))

(def ^:private signed-jwt-backend
  "Primary backend for JWT validation."
  (build-signed-jwt-backend keycloak-config/keycloak-public-key))

(defn wrap-jwt-authentication
  "Middleware to validate the JWT tokens. Multiple backends are supported."
  [handler]
  (let [custom-jwt-signed-backend (build-signed-jwt-backend @jwt-public-key)
        backends (if custom-jwt-signed-backend
                   [signed-jwt-backend custom-jwt-signed-backend] [signed-jwt-backend])]
    (if shared-config/production?
      (apply wrap-authentication handler backends)
      (apply wrap-authentication handler (conj backends signed-jwt-backend-for-testing)))))

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

(>defn member-of-group?
  "Check if group is available in the JWT token."
  [identity group]
  [map? string? :ret boolean?]
  (some #(= group %) (:groups identity)))


;; -----------------------------------------------------------------------------

(defn -main []
  (load-jwt-public-key "https://s3.disqtec.com/on-premise/testing/jwt.key.pub"))

(-main)
