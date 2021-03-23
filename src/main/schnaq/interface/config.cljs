(ns schnaq.interface.config
  (:require [goog.string :as gstring]))

;; Third parameter is a default value
(goog-define api-url "http://localhost:3000")
(goog-define environment "development")
(goog-define build-hash "dev")

(def deleted-statement-text "[deleted]")

(def config
  {:rest-backend api-url
   :environment environment})

(def user-language (atom :de))

(def graph-controversy-upper-bound 65)

(def periodic-update-time
  "Define how many times should the client query the server for live updates.
  Time must be in milliseconds."
  3000)

;; header image
(def place-holder-header-img "https://s3.disqtec.com/schnaq-header-images/header-placeholder.jpg")


;; -----------------------------------------------------------------------------
;; Keycloak

(goog-define keycloak-auth-server-url "https://keycloak.disqtec.com/auth/")
(goog-define keycloak-ssl-required "external")
(goog-define keycloak-realm "development")
(goog-define keycloak-client "development")
(goog-define keycloak-public-client true)
(def keycloak-profile-page
  (gstring/format "https://keycloak.disqtec.com/auth/realms/%s/account/#/personal-info" keycloak-realm))

(def keycloak
  "Keycloak configuration, which is sent to the server via keycloak-js."
  {:url keycloak-auth-server-url
   :realm keycloak-realm
   :clientId keycloak-client})


;; -----------------------------------------------------------------------------
;; Time settings

(def time-settings
  {:pattern "HH:mm dd.MM.yyy"
   :timezone "Europe/Berlin"})