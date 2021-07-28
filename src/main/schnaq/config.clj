(ns schnaq.config
  "General configuration of the schnaq API. Find more configuration settings in
  the schnaq.config.* namespaces."
  (:require [schnaq.config.shared :as shared-config]
            [ghostwheel.core :refer [>defn]]
            [schnaq.toolbelt :as toolbelt]))

(def frontend-url
  (or (System/getenv "FRONTEND_URL") "http://localhost:8700"))

(def summy-url
  "URL to our machine-learning service."
  (or (System/getenv "SUMMY_URL") "http://localhost:8000"))

(>defn summy-urls
  "Return the url to externally call machine learning functions."
  [key]
  [keyword? :ret string?]
  (let [urls {:summary/bart "summary/bart"
              :summary/t5 "summary/t5"}
        url (get urls key)]
    (when url
      (format "%s/%s" summy-url url))))

(def app-codes
  "Set of registered app-codes. Currently hard-coded, maybe dynamic in the future."
  #{"***REMOVED***"})

(def datomic
  "When we are production ready, put here the original production config and use
  dev-locals `divert-system` to use dev-local instead of a datomic cluster."
  {:system "development"
   :server-type :dev-local
   :storage-dir (toolbelt/create-directory! ".datomic/dev-local/data")})

(def db-name (or (System/getenv "DATOMIC_DISCUSSION_DB_NAME") "dev-db"))

(def datomic-uri
  (format (or (System/getenv "DATOMIC_URI") "datomic:dev://localhost:4334/%s")
          db-name))

(def build-hash
  (or (System/getenv "BUILD_HASH") "dev"))

(def deleted-statement-text "[deleted]")

(def email
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "info@schnaq.com")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})

(def profile-picture-height
  "Profile Picture height in pixels."
  200)

(def s3-credentials {:access-key "minio"
                     :secret-key "***REMOVED***"
                     :endpoint shared-config/s3-host
                     :client-config
                     {:path-style-access-enabled true}})
