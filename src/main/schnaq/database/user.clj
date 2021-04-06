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
   :user.registered/first-name
   :user.registered/profile-picture])

(def minimal-user-pattern
  "Minimal user pull pattern."
  [:db/id
   :user/nickname])

(def combined-user-pattern
  (concat registered-user-pattern minimal-user-pattern))

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

(>defn update-groups
  "Updates the user groups to be equal to the new input."
  [keycloak-id groups]
  [:user.registered/keycloak-id :user.registered/groups :ret :user.registered/groups]
  (let [empty-groups [:db/retract [:user.registered/keycloak-id keycloak-id] :user.registered/groups]
        add-new-groups (mapv #(vector :db/add [:user.registered/keycloak-id keycloak-id] :user.registered/groups %)
                             groups)]
    (transact [empty-groups])
    (transact add-new-groups)
    groups))

(>defn register-new-user
  "Registers a new user, when they do not exist already. Depends on the keycloak ID.
  Returns the user, after updating their groups, when they exist."
  [{:keys [id email preferred_username given_name family_name groups]}]
  [associative? :ret ::specs/registered-user]
  (let [existing-user (fast-pull [:user.registered/keycloak-id id] registered-user-pattern)
        temp-id (str "new-registered-user-" id)
        new-user {:db/id temp-id
                  :user.registered/keycloak-id id
                  :user.registered/email email
                  :user.registered/display-name preferred_username
                  :user.registered/first-name given_name
                  :user.registered/last-name family_name
                  :user.registered/groups groups}]
    (if (:db/id existing-user)
      (do
        (update-groups id groups)
        existing-user)
      (-> (transact [(clean-db-vals new-user)])
          (get-in [:tempids temp-id])
          (fast-pull registered-user-pattern)))))

(defn update-display-name
  "Update the name of an existing user."
  [keycloak-id display-name]
  (transact [[:db/add [:user.registered/keycloak-id keycloak-id]
              :user.registered/display-name display-name]])
  (fast-pull [:user.registered/keycloak-id keycloak-id] registered-user-pattern))

(defn update-profile-picture-url
  "Update the profile picture url."
  [keycloak-id profile-picture-url]
  (transact [[:db/add [:user.registered/keycloak-id keycloak-id]
              :user.registered/profile-picture profile-picture-url]])
  (fast-pull [:user.registered/keycloak-id keycloak-id] registered-user-pattern))

(>defn members-of-group
  "Returns all members of a certain group."
  [group-name]
  [::specs/non-blank-string :ret (s/coll-of ::specs/user-or-reference)]
  (flatten
    (query
      '[:find (pull ?users [:db/id
                            :user.registered/display-name])
        :in $ ?group
        :where [?users :user.registered/groups ?group]]
      group-name)))
