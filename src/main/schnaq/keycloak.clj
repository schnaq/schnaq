(ns schnaq.keycloak
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [keycloak.admin :as kc-admin]
            [schnaq.config.keycloak :refer [kc-client realm]]
            [schnaq.database.specs :as specs]))

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
