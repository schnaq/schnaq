(ns schnaq.auth.lib
  (:require [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.user :as user-db]))

(>defn has-role?
  "Check if user has realm-wide role."
  [user-identity roles]
  [map? coll? :ret boolean?]
  (string?
   (some roles (get-in user-identity [:realm_access :roles]))))

(>defn prepare-identity-map
  "Extend identity map parsed from JWT and convert types."
  [request]
  [map? :ret map?]
  (-> request
      (update-in [:identity :sub] str)
      (assoc-in [:identity :id] (str (get-in request [:identity :sub])))
      (assoc-in [:identity :preferred_username] (or (get-in request [:identity :preferred_username])
                                                    (get-in request [:identity :name])))
      (assoc-in [:identity :roles] (get-in request [:identity :realm_access :roles]))
      (assoc-in [:identity :admin?] (has-role? (:identity request) shared-config/admin-roles))))

(>defn beta-tester?
  "Verify that user has a valid beta-tester role in the JWT token."
  [user-identity]
  [map? :ret boolean?]
  (has-role? user-identity shared-config/beta-tester-roles))

(>defn pro-user?
  "Check that user has at least pro-access."
  [user-identity]
  [(? map?) :ret boolean?]
  (if user-identity
    (or (beta-tester? user-identity)
        (user-db/pro-subscription? (:sub user-identity)))
    false))
