(ns schnaq.config
  (:require [schnaq.toolbelt :as toolbelt]))

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

(def email
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "noreply@dialogo.io")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})