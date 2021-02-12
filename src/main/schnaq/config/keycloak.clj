(ns schnaq.config.keycloak
  (:require [buddy.core.keys :as keys]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(def server
  "Define your keycloak server's base url."
  (let [server (or (System/getenv "KEYCLOAK_SERVER") "https://keycloak.disqtec.com")]
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