(ns schnaq.config
  "General configuration of the schnaq API. Find more configuration settings in
  the schnaq.config.* namespaces."
  (:require [schnaq.toolbelt :as toolbelt]))

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
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "info@schnaq.com")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})

(def s3-bucket-headers "schnaq-header-images")

(def s3-bucket-header-url "https://s3.disqtec.com/schnaq-header-images/")

(def s3-credentials {:access-key "minio"
                     :secret-key "***REMOVED***"
                     :endpoint "https://s3.disqtec.com"
                     :client-config
                     {:path-style-access-enabled true}})
