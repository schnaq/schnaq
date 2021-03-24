(ns schnaq.database.user
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn ?]]
            [schnaq.database.specs :as specs]
            [schnaq.meeting.database :refer [transact fast-pull clean-db-vals query]]
            [taoensso.timbre :as log]))

(def ^:private registered-user-pattern
  [:db/id
   :user.registered/keycloak-id
   :user.registered/display-name
   :user.registered/email
   :user.registered/last-name
   :user.registered/first-name])

(def minimal-user-pattern
  "Minimal user pull pattern."
  [:db/id
   :user/nickname])

(def combined-user-pattern
  (conj registered-user-pattern))

(>defn add-user
  "Add a new anonymous user / author to the database."
  [nickname]
  [string? :ret int?]
  (when (s/valid? :user/nickname nickname)
    (get-in
      (transact [{:db/id "temp-user"
                  :user/nickname nickname}])
      [:tempids "temp-user"])))

(>defn user-by-nickname
  "Return the **schnaq** user-id by nickname. The nickname is not case sensitive.
  If there is no user with said nickname returns nil."
  [nickname]
  [string? :ret (? number?)]
  (ffirst
    (query
      '[:find ?user
        :in $ ?user-name
        :where [?user :user/nickname ?original-nickname]
        [(.toLowerCase ^String ?original-nickname) ?lower-name]
        [(= ?lower-name ?user-name)]]
      (.toLowerCase ^String nickname))))

(>defn canonical-username
  "Return the canonical username (regarding case) that is saved."
  [nickname]
  [:user/nickname :ret :user/nickname]
  (:user/nickname
    (fast-pull (user-by-nickname nickname) [:user/nickname])))

(>defn add-user-if-not-exists
  "Adds a user if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:user/nickname :ret int?]
  (if-let [user-id (user-by-nickname nickname)]
    user-id
    (do (log/info "Added a new user: " nickname)
        (add-user nickname))))

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
