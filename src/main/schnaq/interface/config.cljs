(ns schnaq.interface.config
  (:require [goog.string :as gstring]
            [schnaq.config.shared :as shared-config]))

;; Second parameter is a default value
(goog-define build-hash "dev")

(def deleted-statement-text "[deleted]")

(def user-language (atom :de))

(def graph-controversy-upper-bound 65)

(def default-anonymous-display-name "Anonymous")

(def periodic-update-time
  "Define how many times should the client query the server for live updates.
  Time must be in milliseconds."
  3000)

(def place-holder-header-img "https://s3.schnaq.com/schnaq-header-images/header-placeholder.jpg")

(def in-iframe?
  "Check if schnaq is loaded in an iframe."
  false)

;; -----------------------------------------------------------------------------
;; Pricing

(def pricing-pro-tier
  "Price in euros, exclusively VAT."
  6.99)

;; -----------------------------------------------------------------------------
;; Keycloak

(goog-define keycloak-realm "development")
(goog-define keycloak-client "development")
(def keycloak-profile-page
  (gstring/format "%s/auth/realms/%s/account/#/personal-info" shared-config/keycloak-host keycloak-realm))

(def keycloak
  "Keycloak configuration, which is sent to the server via keycloak-js."
  {:url (gstring/format "%s/auth/" shared-config/keycloak-host)
   :realm keycloak-realm
   :clientId keycloak-client})

;; -----------------------------------------------------------------------------

(def max-allowed-profile-picture-size
  "Maximal allowed image size of profile picture in bytes."
  5000000)

(def marketing-num-schnaqs 600)
(def marketing-num-statements 4000)

;; -----------------------------------------------------------------------------
;; Example schnaqs and statements
;;
;; These samples default to the staging environment so that it works on staging
;; and on production. Change these symbols to your dev environment accordingly.
;; Replaced by the CI when building the application for production.

(goog-define example-share-hash "6586e787-8704-4b4b-9221-5821d15626b5")
(goog-define example-api-url "https://api.staging.schnaq.com")
(goog-define example-statement 17592186049502)

;; -----------------------------------------------------------------------------
;; Stripe

(goog-define stripe-product-price-id-schnaq-pro "price_1K9S66FrKCGqvoMokD1SoBic")