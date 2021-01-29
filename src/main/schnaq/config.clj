(ns schnaq.config
  (:require [buddy.core.keys :as keys]
            [schnaq.toolbelt :as toolbelt]))

;; Dev config. Need a proper way to handle switch when in production.
;; ##################################################################
(def datomic
  "When we are production ready, put here the original production config and use
  dev-locals `divert-system` to use dev-local instead of a datomic cluster."
  {:system "development"
   :server-type :dev-local
   :storage-dir (toolbelt/create-directory! ".datomic/dev-local/data")})

(def db-name (or (System/getenv "DATOMIC_DISCUSSION_DB_NAME") "dev-db"))

(def api
  {:port (Integer/parseInt (or (System/getenv "API_PORT") "3000"))})

(def admin-password
  (or (System/getenv "ADMIN_PASSWORD") "Schnapspralinen"))

(def env-mode
  (or (System/getenv "ENVIRONMENT") "development"))

(def build-hash
  (or (System/getenv "BUILD_HASH") "dev"))

(def deleted-statement-text "[deleted]")

(def email
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "noreply@dialogo.io")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})

(def s3-bucket-headers "schnaq-header-images")

(def s3-bucket-header-url "https://s3.disqtec.com/schnaq-header-images/")

(def s3-credentials {:access-key "minio"
                     :secret-key "***REMOVED***"
                     :endpoint "https://s3.disqtec.com"
                     :client-config
                     {:path-style-access-enabled true}})

;; -----------------------------------------------------------------------------
;; Keycloak

(def keycloak-public-key
  "Public RSA key of keycloak client."
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4icNzKKV/FhHFmYTRZGp28Gc7Vk4TAsCRsdb3w2Mv7EUzAOgdB0/I+VluvIN7u9wwohdg+UThjtQYY4tW036AefBgG6qdjpnGkxovmx1tlPEloR0fcnFUyB19XCQVhWOktnvQ45MB/O7VcxK14+A0t16iBcULT91mnqeKm08WiFgdU7s2qONPfDsMGOH7XBX4RNiBshxEqlpmAMvyxDHitqzt2bLJ9E08OCNbFKjXCMc763E/t90/RqEVGQsMGAu49Pi12r2ZszJYwZRMNYl0EJIfPeX4g9q0BdcuqKGvnjwyllFOswD/NE5htnLhJUP8aNS+45iHysMwyy7AuhvEwIDAQAB\n-----END PUBLIC KEY-----"))