(ns schnaq.database.user-deletion
  (:require [com.fulcrologic.guardrails.core :refer [>defn =>]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]))

(>defn delete-all-statements-for-user
  "Deletes all statements for a user."
  [keycloak-id]
  [:user.registered/keycloak-id => any?]
  (let [statement-ids (map :db/id (discussion-db/all-statements-from-user keycloak-id))]
    (discussion-db/delete-statements! statement-ids)))

(>defn delete-discussions-for-user
  "Deletes all discussions for a given user."
  [keycloak-id]
  [:user.registered/keycloak-id => any?]
  (let [share-hashes (map :discussion/share-hash (discussion-db/discussions-from-user keycloak-id))]
    (run! discussion-db/delete-discussion share-hashes)))

(>defn delete-user-identity
  "Deletes a user's personal information. 
  Keeps db/id and keycloak-id, but retracts all other information and renames
  the user."
  [keycloak-id]
  [:user.registered/keycloak-id => any?]
  (let [reduced-user (dissoc (main-db/fast-pull [:user.registered/keycloak-id keycloak-id])
                             :db/id
                             :user.registered/keycloak-id)]
    (main-db/transact
     (conj
      (vec
       (for [k (keys reduced-user)]
         [:db/retract [:user.registered/keycloak-id keycloak-id] k]))
      [:db/add [:user.registered/keycloak-id keycloak-id]
       :user.registered/display-name
       shared-config/default-anonymous-display-name]))))
