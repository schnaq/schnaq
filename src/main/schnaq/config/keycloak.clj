(ns schnaq.config.keycloak
  (:require [buddy.core.keys :as keys]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [keycloak.deployment :refer [keycloak-client client-conf]]
            [schnaq.config.shared :as shared-config]
            [taoensso.timbre :as log]))

(def server
  "Define your keycloak server's base url."
  (let [server shared-config/keycloak-host]
    (if (string/ends-with? server "/")
      server
      (format "%s/" server))))

(def realm
  "Specify the realm you are connecting to."
  (or (System/getenv "KEYCLOAK_REALM") "development"))

(defn- get-public-key
  "Query keycloak server to receive the current public-key."
  []
  (-> (format "%sauth/realms/%s" server realm)
      (client/get {:accept :json})
      :body
      (json/read-str :key-fn keyword)
      :public_key))

(def backend-client-id (or (System/getenv "KEYCLOAK_CLIENT_ID") "development-backend"))

(def backend-client-secret (or (System/getenv "KEYCLOAK_CLIENT_SECRET") "17ce03e6-8dd4-4728-b27b-41693c2bd6d7"))

(def kc-client
  (-> (client-conf {:auth-server-url (format "%sauth/" server)
                    :realm realm
                    :client-id "admin-cli"})
      (keycloak-client backend-client-id backend-client-secret)))

;; -----------------------------------------------------------------------------

(def keycloak-public-key
  "Build public key from server's response."
  (try
    (let [public-key (get-public-key)]
      (log/info "[Keycloak] Successfully loaded public key:" public-key)
      (keys/str->public-key
        (format "-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----"
                public-key)))
    (catch Exception _
      (log/error (format "[Keycloak] Keycloak server unreachable or realm does not exist. Your settings: server: %s, realm: %s"
                         server realm)))))