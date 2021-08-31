(ns schnaq.config
  "General configuration of the schnaq API. Find more configuration settings in
  the schnaq.config.* namespaces."
  (:require [buddy.core.keys :as keys]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [schnaq.config.shared :as shared-config]
            [schnaq.config.summy :as summy-config]))

(def frontend-url
  (or (System/getenv "FRONTEND_URL") "http://localhost:8700"))

(def frontend-host
  "Parse the host (and port) from the url to allow it in CORS."
  (let [url (io/as-url frontend-url)
        host (.getHost url)
        port (.getPort url)]
    (if (or (= 80 port) (= 443 port) (= -1 port))           ;; .getPort returns -1 if no port is explicitly configured, e.g. at https://schnaq.com
      host
      (format "%s:%d" host port))))

(def additional-cors
  "Define additional allowed origins for CORS. (ONLY USE ONES THAT ARE TRUSTED)"
  (str/split
    (or (System/getenv "ADDITIONAL_CORS_ORIGINS") "")
    #","))

(def app-codes
  "Set of registered app-codes. Currently hard-coded, maybe dynamic in the future."
  #{summy-config/app-code})

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


;; -----------------------------------------------------------------------------
;; S3 Configuration

(def ^:private s3-access-key
  (or (System/getenv "S3_ACCESS_KEY") "minio"))

(def ^:private s3-secret-key
  (or (System/getenv "S3_SECRET_KEY") "***REMOVED***"))

(def s3-credentials {:access-key s3-access-key
                     :secret-key s3-secret-key
                     :endpoint shared-config/s3-host
                     :client-config {:path-style-access-enabled true}})


;; -----------------------------------------------------------------------------
;; JWT

(def testing-private-key
  (keys/str->private-key
    (slurp "https://s3.disqtec.com/on-premise/testing/jwt.key")))
(def testing-public-key
  (keys/str->public-key
    (slurp "https://s3.disqtec.com/on-premise/testing/jwt.key.pub")))
