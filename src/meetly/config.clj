(ns meetly.config
  (:require [meetly.toolbelt :as toolbelt]))

;; Dev config. Need a proper way to handle switch when in production.
;; ##################################################################
(def datomic
  "When we are production ready, put here the original production config and use
  dev-locals `divert-system` to use dev-local instead of a datomic cluster."
  {:system "development"
   :server-type :dev-local
   :storage-dir (toolbelt/create-directory! ".datomic/dev-local/data")})

(def db-name (or (System/getenv "DATOMIC_DISCUSSION_DB_NAME") "dev-db"))

(def rest-api
  {:port (Integer/parseInt (or (System/getenv "API_PORT") "3000"))
   :url (or (System/getenv "REST_API_URL") ".*")})