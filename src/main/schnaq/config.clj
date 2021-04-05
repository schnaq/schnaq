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

(def env-mode
  (or (System/getenv "ENVIRONMENT") "development"))

(def build-hash
  (or (System/getenv "BUILD_HASH") "dev"))

(def deleted-statement-text "[deleted]")

(def email
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "info@schnaq.com")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})

(def s3-base
  (or (System/getenv "S3_HOST") "https://s3.disqtec.com"))

(defn s3-buckets
  "Returns bucket names"
  [bucket-name]
  (get
    {:schnaq/header-images "schnaq-header-images"
     :user/profile-pictures "schnaq-profile-pictures"
     :feedback/screenshots "schnaq-feedback-screenshots"}
    bucket-name))

(def s3-credentials {:access-key "minio"
                     :secret-key "***REMOVED***"
                     :endpoint "https://s3.disqtec.com"
                     :client-config
                     {:path-style-access-enabled true}})
