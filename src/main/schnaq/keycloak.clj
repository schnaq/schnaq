(ns schnaq.keycloak
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [keycloak.admin :as kc-admin]
            [keycloak.user :as kc-user]
            [schnaq.config.keycloak :refer [kc-client realm]]
            [schnaq.database.specs :as specs]))

;; -----------------------------------------------------------------------------
;; Groups

(>defn get-group-id
  "Return the internal ID of the group in Keycloak."
  [group]
  [:hub/keycloak-name => ::specs/uuid-str]
  (kc-admin/get-group-id kc-client realm group))

(>defn add-user-to-group!
  "Add user to a keycloak group. Returns an object  
  `org.keycloak.representations.idm.GroupRepresentation`"
  [group-id new-user-keycloak-id]
  [::specs/uuid-str :user.registered/keycloak-id => any?]
  (kc-admin/add-user-to-group! kc-client realm group-id new-user-keycloak-id))

;; -----------------------------------------------------------------------------
;; Users

(>defn get-user
  "Return a user object based on its keycloak-id."
  [keycloak-id]
  [:user.registered/keycloak-id => any?]
  (kc-user/get-user kc-client realm keycloak-id))

(>defn add-realm-roles!
  "Add realm roles to a user. Must be valid roles defined for the realm in
  keycloak. Only adds valid roles to the users. No new roles are created.
   
  Example: `(add-realm-roles! \"00000000-0000-0000-0000-000000000000\" [\"pro\"])`"
  [keycloak-id roles]
  [:user.registered/keycloak-id (s/coll-of string?) => (s/coll-of string?)]
  (kc-user/add-realm-roles! kc-client realm (.getUsername (get-user keycloak-id)) roles))
