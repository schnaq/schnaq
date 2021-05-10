(ns schnaq.database.user
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.database.main :refer [transact fast-pull clean-db-vals query]]
            [schnaq.database.specs :as specs]
            [taoensso.timbre :as log]))

(def registered-user-pattern
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
      @(transact [{:db/id "temp-user"
                   :user/nickname nickname}])
      [:tempids "temp-user"])))

(>defn user-by-nickname
  "Return the **schnaq** user-id by nickname. The nickname is not case sensitive.
  If there is no user with said nickname returns nil."
  [nickname]
  [string? :ret (? number?)]
  (query
    '[:find ?user .
      :in $ ?user-name
      :where [?user :user/nickname ?original-nickname]
      [(.toLowerCase ^String ?original-nickname) ?lower-name]
      [(= ?lower-name ?user-name)]]
    (.toLowerCase ^String nickname)))

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

(defn- update-user-info
  "Updates given-name, last-name, email-address when they are not nil."
  [{:keys [id given_name family_name email]} existing-user]
  (let [user-ref [:user.registered/keycloak-id id]
        transaction (cond->
                      []
                      (and given_name
                           (not= given_name (:user.registered/first-name existing-user)))
                      (conj [:db/add user-ref :user.registered/first-name given_name])
                      (and family_name
                           (not= family_name (:user.registered/last-name existing-user)))
                      (conj [:db/add user-ref :user.registered/last-name family_name])
                      (and email
                           (not= email (:user.registered/email existing-user)))
                      (conj [:db/add user-ref :user.registered/email email]))]
    (when (seq transaction)
      (transact transaction))))

(>defn register-new-user
  "Registers a new user, when they do not exist already. Depends on the keycloak ID.
  Returns the user, after updating their groups, when they exist. Returns a tuple which contains
  whether the user is newly created and the user entity itself."
  [{:keys [id email preferred_username given_name family_name groups] :as identity}]
  [associative? :ret (s/tuple boolean? ::specs/registered-user)]
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
        (update-user-info identity existing-user)
        (update-groups id groups)
        [false existing-user])
      [true (-> @(transact [(clean-db-vals new-user)])
                (get-in [:tempids temp-id])
                (fast-pull registered-user-pattern))])))

(>defn- update-user-field
  "Updates a user's field in the database and return updated user."
  [keycloak-id field value]
  [:user.registered/keycloak-id keyword? any? :ret ::specs/registered-user]
  (let [new-db (:db-after
                 @(transact [[:db/add [:user.registered/keycloak-id keycloak-id]
                              field value]]))]
    (fast-pull [:user.registered/keycloak-id keycloak-id] registered-user-pattern new-db)))

(>defn update-display-name
  "Update the name of an existing user."
  [keycloak-id display-name]
  [:user.registered/keycloak-id string? :ret ::specs/registered-user]
  (update-user-field keycloak-id :user.registered/display-name display-name))

(>defn update-profile-picture-url
  "Update the profile picture url."
  [keycloak-id profile-picture-url]
  [:user.registered/keycloak-id :user.registered/profile-picture :ret ::specs/registered-user]
  (update-user-field keycloak-id :user.registered/profile-picture profile-picture-url))

(>defn members-of-group
  "Returns all members of a certain group."
  [group-name]
  [::specs/non-blank-string :ret (s/coll-of ::specs/user-or-reference)]
  (query
    '[:find [(pull ?users combined-user-pattern) ...]
      :in $ ?group combined-user-pattern
      :where [?users :user.registered/groups ?group]]
    group-name combined-user-pattern))
