(ns schnaq.database.user-deletion
  (:require [com.fulcrologic.guardrails.core :refer [>defn =>]]
            [schnaq.database.discussion :as discussion-db]))

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
