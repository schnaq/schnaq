(ns schnaq.interface.config)

;; Third parameter is a default value
(goog-define api-url "http://localhost:3000")
(goog-define environment "development")
(goog-define build-hash "dev")
(goog-define spotlight-1 "https://schnaq.com/schnaq/7d6f68cb-ac6c-4407-ba86-ada29b2abb3b/")
(goog-define spotlight-2 "https://schnaq.com/schnaq/0950ab05-0edb-441e-b8ca-f8907e85c5c6/")
(goog-define spotlight-3 "https://schnaq.com/schnaq/ed5788b6-11da-4016-bb24-a93646705739/")
(goog-define keycloak-realm "schnaq")
(goog-define keycloak-auth-server-url "https://keycloak.disqtec.com/auth/")
(goog-define keycloak-ssl-required "external")
(goog-define keycloak-resource "development")
(goog-define keycloak-public-client true)

(def keycloak
  {:url keycloak-auth-server-url
   :realm keycloak-realm
   :clientId keycloak-resource})

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
