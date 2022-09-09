(ns schnaq.config
  "General configuration of the schnaq API. Find more configuration settings in
  the schnaq.config.* namespaces."
  (:require [buddy.core.keys :as keys]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [schnaq.config.shared :as shared-config]
            [schnaq.config.summy :as summy-config]))

(def pro-email-hosts
  "Define email-hosts which should automatically be assigned a pro-role.
  Environment variable should be comma-separated to be parsable.
   
  Example: `hhu.de,schnaq.com,razupaltu.ff`"
  (when-let [hosts (System/getenv "PRO_EMAIL_HOSTS")]
    (set (str/split hosts #", ?"))))

(def frontend-url
  (or (System/getenv "FRONTEND_URL") "http://localhost:8700"))

(def frontend-host
  "Parse the host (and port) from the url to allow it in CORS."
  (let [url (io/as-url frontend-url)
        host (.getHost url)
        port (.getPort url)]
    (if (or (= 80 port) (= 443 port) (= -1 port)) ;; .getPort returns -1 if no port is explicitly configured, e.g. at https://schnaq.com
      host
      (format "%s:%d" host port))))

(def additional-cors
  "Define additional allowed origins for CORS. (ONLY USE ONES THAT ARE TRUSTED)"
  (str/split
   (or (System/getenv "ADDITIONAL_CORS_ORIGINS") "")
   #","))

(def routes-without-csrf-check
  "Collection of route-names, where the middleware does not check for our csrf
  header. Commonly used for incoming requests from external services, like
  stripe."
  #{:api.stripe/webhook :api.ws/post})

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

(def notification-blacklist
  "Define for which share-hashes we do not send notification mails."
  #{"e8f54922-0d88-4953-8f43-ddc819d7f201" ;; production schnaq FAQs
    "6586e787-8704-4b4b-9221-5821d15626b5" ;; staging schnaq FAQs
    })

(def email
  {:sender-address (or (System/getenv "EMAIL_SENDER_ADDRESS") "info@schnaq.com")
   :sender-host (or (System/getenv "EMAIL_HOST") "smtp.ionos.de")
   :sender-password (System/getenv "EMAIL_PASSWORD")})

(def mail-template "https://s3.schnaq.com/email/templates/generic-mail.html")
(def mail-content-button-right-template "https://s3.schnaq.com/email/templates/snippets/content-left-button-right.html")

(def mattermost-webhook-url
  "URL to mattermost-webhook to post news to the chat."
  (or (System/getenv "MATTERMOST_WEBHOOK_URL")
      "***REMOVED***"))

;; -----------------------------------------------------------------------------
;; Images

(def profile-picture-width
  "Profile Picture width in pixels."
  200)

(def image-max-width-logo
  "Set the maximum image-width of the logo in pixels."
  500)

(def image-max-width-header
  "Set the maximum image-width of the header in pixels."
  1000)

(def image-width-in-statement
  "Set maximum width of an image used, e.g. in a statement in pixels."
  500)

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
   (slurp "https://s3.schnaq.com/on-premise/testing/jwt.key")))
(def testing-public-key
  (keys/str->public-key
   (slurp "https://s3.schnaq.com/on-premise/testing/jwt.key.pub")))

;; -----------------------------------------------------------------------------
;; Stripe

(def stripe-secret-api-key
  (or (System/getenv "STRIPE_SECRET_KEY")
      "***REMOVED***"))
(def stripe-webhook-access-key
  (or (System/getenv "STRIPE_WEBHOOK_ACCESS_KEY")
      "***REMOVED***"))
