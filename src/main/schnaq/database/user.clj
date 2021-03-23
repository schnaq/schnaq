(ns schnaq.database.user
  (:require [ghostwheel.core :refer [>defn]]
            [schnaq.database.specs :as specs]
            [schnaq.meeting.database :refer [transact fast-pull clean-db-vals]]))

(def ^:private registered-user-pattern
  [:db/id
   :user.registered/keycloak-id
   :user.registered/display-name
   :user.registered/email
   :user.registered/last-name
   :user.registered/first-name])

(>defn register-new-user
  "Registers a new user, when they do not exist already. Depends on the keycloak ID.
  Returns the user, when they exist."
  [identity-map]
  [associative? :ret ::specs/registered-user]
  (let [{:keys [id email preferred_username given_name family_name]} identity-map
        existing-user (fast-pull [:user.registered/keycloak-id id] registered-user-pattern)
        temp-id (str "new-registered-user-" id)
        new-user {:db/id temp-id
                  :user.registered/keycloak-id id
                  :user.registered/email email
                  :user.registered/display-name preferred_username
                  :user.registered/first-name given_name
                  :user.registered/last-name family_name}]
    (if (:db/id existing-user)
      existing-user
      (-> (transact [(clean-db-vals new-user)])
          (get-in [:tempids temp-id])
          (fast-pull registered-user-pattern)))))
