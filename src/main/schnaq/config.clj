(ns schnaq.config
  "General configuration of the schnaq API. Find more configuration settings in
  the schnaq.config.* namespaces."
  (:require [buddy.core.keys :as keys]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [config.core :refer [env]]
            [schnaq.config.shared :as shared-config]))

(def frontend-url
  (or (:frontend-url env) "http://localhost:8700"))

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
   (or (:additional-cors-origins env) "")
   #","))

(def routes-without-csrf-check
  "Collection of route-names, where the middleware does not check for our csrf
  header. Commonly used for incoming requests from external services."
  #{:api.ws/post})

(def app-codes
  "Set of registered app-codes. Currently hard-coded, maybe dynamic in the future."
  #{(:app-code env)})

(def db-name (or (:datomic-discussion-db-name env) "dev-db"))

(def datomic-uri
  (format (or (:datomic-uri env) "datomic:dev://localhost:4334/%s")
          db-name))

(def build-hash
  (or (:build-hash env) "dev"))

(def deleted-statement-text "[deleted]")

(def notification-blacklist
  "Define for which share-hashes we do not send notification mails."
  #{"e8f54922-0d88-4953-8f43-ddc819d7f201" ;; production schnaq FAQs
    "6586e787-8704-4b4b-9221-5821d15626b5" ;; staging schnaq FAQs
    })

(def email
  {:sender-address (:email-sender-address env)
   :sender-host (:email-host env)
   :sender-port (Integer/parseInt (or (:email-port env) "465"))
   :sender-username (:email-username env)
   :sender-password (:email-password env)})

(def mail-template "https://snq-common.s3.nl-ams.scw.cloud/email/templates/generic-mail.html")
(def mail-content-button-right-template "https://snq-common.s3.nl-ams.scw.cloud/email/templates/snippets/content-left-button-right.html")

(def sentry-dsn
  "DSN of the Sentry project collecting the backend's errors. Error tracking is
  disabled when this is not configured."
  (:sentry-dsn env))

(def sentry-environment
  "Environment reported to Sentry. Falls back to the general environment, but is
  configurable on its own: a staging deployment may need to run as
  `production` to keep the dev-only routes disabled, while its errors should
  still be told apart from the real production ones in Sentry."
  (or (:sentry-environment env) shared-config/environment))

(def mattermost-webhook-url
  "URL to mattermost-webhook to post news to the chat."
  (:mattermost-webhook-url env))

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

(def s3-credentials {:access-key (or (:s3-access-key env) "configureme")
                     :secret-key (or (:s3-secret-key env) "configureme")
                     :region (or (:s3-region env) "configureme")
                     :endpoint shared-config/s3-host
                     :client-config {:path-style-access-enabled true}})

;; -----------------------------------------------------------------------------
;; JWT

(def testing-private-key
  (keys/str->private-key
   (slurp (io/resource "testing/jwt.key"))))
(def testing-public-key
  (keys/str->public-key
   (slurp (io/resource "testing/jwt.key.pub"))))
