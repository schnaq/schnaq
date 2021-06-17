(ns schnaq.interface.config
  (:require [goog.string :as gstring]
            [schnaq.config.shared :as shared-config]))

;; Second parameter is a default value
(goog-define environment "development")
(goog-define build-hash "dev")

(def deleted-statement-text "[deleted]")

(def config
  {:rest-backend shared-config/api-url
   :environment environment})

(def user-language (atom :de))

(def graph-controversy-upper-bound 65)

(def default-anonymous-display-name "Anonymous")

(def periodic-update-time
  "Define how many times should the client query the server for live updates.
  Time must be in milliseconds."
  3000)

(def place-holder-header-img "https://s3.disqtec.com/schnaq-header-images/header-placeholder.jpg")


;; -----------------------------------------------------------------------------
;; Keycloak

(goog-define keycloak-ssl-required "external")
(goog-define keycloak-realm "development")
(goog-define keycloak-client "development")
(goog-define keycloak-public-client true)
(def keycloak-profile-page
  (gstring/format "%s/auth/realms/%s/account/#/personal-info" shared-config/keycloak-host keycloak-realm))

(def keycloak
  "Keycloak configuration, which is sent to the server via keycloak-js."
  {:url (gstring/format "%s/auth/" shared-config/keycloak-host)
   :realm keycloak-realm
   :clientId keycloak-client})


;; -----------------------------------------------------------------------------
;; Time settings

(def time-settings
  {:pattern "HH:mm dd.MM.yyy"
   :timezone "Europe/Berlin"})

;; -----------------------------------------------------------------------------
;; Time settings

(def max-allowed-profile-picture-size
  "Maximal allowed image size of profile picture in bytes."
  5000000)

(def marketing-num-schnaqs 430)
(def marketing-num-statements 2850)