(ns schnaq.database.user
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ?]]
            [schnaq.database.main :refer [fast-pull query transact]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.shared-toolbelt :refer [remove-nil-values-from-map]]
            [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Query user(s) from database

(>defn user-by-nickname
  "Return the **schnaq** user-id by nickname. The nickname is not case-sensitive.
  If there is no user with said nickname returns nil."
  [nickname]
  [:user/nickname :ret (? :db/id)]
  (query
   '[:find ?user .
     :in $ ?user-name
     :where [?user :user/nickname ?original-nickname]
     [(.toLowerCase ^String ?original-nickname) ?lower-name]
     [(= ?lower-name ?user-name)]]
   (.toLowerCase ^String nickname)))

(>defn user-by-email
  "Returns the registered user by email."
  [user-email]
  [:user.registered/email :ret (? ::specs/registered-user)]
  (let [user (fast-pull [:user.registered/email user-email]
                        patterns/public-user)]
    (when (:db/id user)
      user)))

(>defn private-user-by-keycloak-id
  "Returns the registered user by email."
  [keycloak-id]
  [:user.registered/keycloak-id :ret ::specs/registered-user]
  (fast-pull [:user.registered/keycloak-id keycloak-id]
             patterns/private-user))

(>defn all-registered-users
  "Returns all registered users."
  []
  [:ret (s/coll-of ::specs/registered-user)]
  (query
   '[:find [(pull ?registered-user user-pattern) ...]
     :in $ user-pattern
     :where [?registered-user :user.registered/keycloak-id _]]
   patterns/private-user))

(>defn users-by-notification-interval
  "Query users from database matching the notification interval."
  [interval]
  [:user.registered/notification-mail-interval :ret (s/coll-of ::specs/registered-user)]
  (query
   '[:find [(pull ?users pattern) ...]
     :in $ ?interval pattern
     :where [?users :user.registered/notification-mail-interval ?interval]]
   interval patterns/private-user))

;; -----------------------------------------------------------------------------

(>defn add-user
  "Add a new anonymous user / author to the database."
  [nickname]
  [:user/nickname :ret :db/id]
  (when (s/valid? :user/nickname nickname)
    (get-in
     @(transact [{:db/id "temp-user"
                  :user/nickname nickname}])
     [:tempids "temp-user"])))

(>defn add-user-if-not-exists
  "Adds a user if they do not exist yet. Returns the (new) user-id."
  [nickname]
  [:user/nickname :ret :db/id]
  (if-let [user-id (user-by-nickname nickname)]
    user-id
    (do (log/info "Added a new user:" nickname)
        (add-user nickname))))

(defn user-id
  "Returns the user-id of the passed user. Takes a username and a keycloak-id that may be nil.
  Returns the keycloak-user if logged-in, otherwise the anon user-id."
  [display-name keycloak-id]
  (if keycloak-id
    (:db/id (fast-pull [:user.registered/keycloak-id keycloak-id] [:db/id]))
    (add-user-if-not-exists display-name)))

(>defn update-groups
  "Updates the user groups to be equal to the new input."
  [keycloak-id groups]
  [:user.registered/keycloak-id (? :user.registered/groups) :ret (? :user.registered/groups)]
  (when groups
    (let [empty-groups [:db/retract [:user.registered/keycloak-id keycloak-id] :user.registered/groups]
          add-new-groups (mapv #(vector :db/add [:user.registered/keycloak-id keycloak-id] :user.registered/groups %)
                               groups)]
      (transact [empty-groups])
      (transact add-new-groups)
      groups)))

(defn update-visited-schnaqs
  "Updates the user's visited schnaqs by adding the new ones. Input is a user-id and a collection of valid ids."
  [keycloak-id visited-schnaqs]
  (let [txs (mapv #(vector :db/add [:user.registered/keycloak-id keycloak-id] :user.registered/visited-schnaqs %)
                  visited-schnaqs)]
    (transact txs)))

(>defn remove-visited-schnaq
  "Remove a visited schnaq from a user."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret map?]
  @(transact [[:db/retract [:user.registered/keycloak-id keycloak-id]
               :user.registered/visited-schnaqs [:discussion/share-hash share-hash]]]))

(>defn archive-schnaq
  "Persist share-hash to a user's archived."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret map?]
  @(transact [[:db/add [:user.registered/keycloak-id keycloak-id]
               :user.registered/archived-schnaqs [:discussion/share-hash share-hash]]]))

(>defn unarchive-schnaq
  "Remove a schnaq from the user's archived schnaq."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret map?]
  @(transact [[:db/retract [:user.registered/keycloak-id keycloak-id]
               :user.registered/archived-schnaqs [:discussion/share-hash share-hash]]]))

(defn seen-statements-entity
  "Returns the entity-id that a certain user / discussion combination has for seen statements."
  [keycloak-id discussion-hash]
  (query '[:find ?seen-statement .
           :in $ ?keycloak-id ?discussion-hash
           :where [?user :user.registered/keycloak-id ?keycloak-id]
           [?seen-statement :seen-statements/user ?user]
           [?seen-statement :seen-statements/visited-schnaq ?discussion]
           [?discussion :discussion/share-hash ?discussion-hash]]
         keycloak-id discussion-hash))

(>defn known-statement-ids
  "Returns known-statement ids for a user and a certain share-hash as a set."
  [keycloak-id share-hash]
  [:user.registered/keycloak-id :discussion/share-hash :ret (s/coll-of :db/id)]
  (set
   (query '[:find [?visited-statements ...]
            :in $ ?keycloak-id ?discussion-hash
            :where [?user :user.registered/keycloak-id ?keycloak-id]
            [?seen-statement :seen-statements/user ?user]
            [?seen-statement :seen-statements/visited-schnaq ?discussion]
            [?discussion :discussion/share-hash ?discussion-hash]
            [?seen-statement :seen-statements/visited-statements ?visited-statements]]
          keycloak-id share-hash)))

(>defn create-visited-statements-for-discussion
  [keycloak-id discussion-hash visited-statements]
  [:user.registered/keycloak-id :discussion/share-hash (s/coll-of :db/id) :ret future?]
  (let [queried-id (seen-statements-entity keycloak-id discussion-hash)
        temp-id (or queried-id (str "seen-statements-" keycloak-id "-" discussion-hash))
        new-visited {:db/id temp-id
                     :seen-statements/user [:user.registered/keycloak-id keycloak-id]
                     :seen-statements/visited-schnaq [:discussion/share-hash discussion-hash]
                     :seen-statements/visited-statements visited-statements}]
    (transact [(remove-nil-values-from-map new-visited)])))

(defn update-visited-statements
  "Updates the user's visited statements by adding the new ones."
  [keycloak-id share-hash-statement-ids]
  [:user.registered/keycloak-id ::specs/share-hash-statement-id-mapping :ret nil?]
  (doseq [[discussion-hash statement-ids] share-hash-statement-ids]
    (create-visited-statements-for-discussion keycloak-id discussion-hash statement-ids)))

(defn- update-user-info
  "Updates given-name, last-name, email-address when they are not nil."
  [{:keys [id given_name family_name email avatar]} existing-user]
  (let [user-ref [:user.registered/keycloak-id id]
        transaction
        (cond-> []
          (and given_name
               (not= given_name (:user.registered/first-name existing-user)))
          (conj [:db/add user-ref :user.registered/first-name given_name])
          (and family_name
               (not= family_name (:user.registered/last-name existing-user)))
          (conj [:db/add user-ref :user.registered/last-name family_name])
          (and email
               (not= email (:user.registered/email existing-user)))
          (conj [:db/add user-ref :user.registered/email email])
          (and avatar
               (not= avatar (:user.registered/profile-picture existing-user)))
          (conj [:db/add user-ref :user.registered/profile-picture avatar]))]
    (when (seq transaction)
      (transact transaction))))

(>defn register-new-user
  "Registers a new user, when they do not exist already. Depends on the keycloak ID.
  Returns the user, after updating their groups, when they exist. Returns a tuple which contains
  whether the user is newly created and the user entity itself."
  [{:keys [sub email preferred_username given_name family_name groups avatar] :as identity} visited-schnaqs visited-statements]
  [associative? (s/coll-of :db/id) (s/coll-of :db/id) :ret (s/tuple boolean? ::specs/registered-user)]
  (let [id (str sub)
        existing-user (fast-pull [:user.registered/keycloak-id id] patterns/private-user)
        temp-id (str "new-registered-user-" id)
        new-user {:db/id temp-id
                  :user.registered/keycloak-id id
                  :user.registered/email email
                  :user.registered/display-name preferred_username
                  :user.registered/first-name given_name
                  :user.registered/last-name family_name
                  :user.registered/groups groups
                  :user.registered/profile-picture avatar
                  :user.registered/notification-mail-interval :notification-mail-interval/never
                  :user.registered/visited-schnaqs visited-schnaqs}]
    (if (:db/id existing-user)
      (do
        (update-user-info identity existing-user)
        (update-groups id groups)
        (update-visited-schnaqs id visited-schnaqs)
        (when-not (nil? visited-statements)
          (update-visited-statements id visited-statements))
        [false existing-user])
      (let [new-user-from-db (-> @(transact [(remove-nil-values-from-map new-user)])
                                 (get-in [:tempids temp-id])
                                 (fast-pull patterns/public-user))]
        (when-not (nil? visited-statements)
          (update-visited-statements (:user.registered/keycloak-id new-user-from-db) visited-statements))
        [true new-user-from-db]))))

(defn update-user
  "Update an existing user. Throw in all changed fields."
  [{:user.registered/keys [keycloak-id] :as user}]
  (let [user' (assoc user :db/id [:user.registered/keycloak-id keycloak-id])
        tx-result @(transact [user'])]
    (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user (:db-after tx-result))))

(>defn- add-user-field
  "Updates a user's field in the database and return updated user."
  ([keycloak-id field value]
   [:user.registered/keycloak-id keyword? any? :ret ::specs/registered-user]
   (add-user-field keycloak-id field value patterns/public-user))
  ([keycloak-id field value pattern]
   [:user.registered/keycloak-id keyword? any? any? :ret ::specs/registered-user]
   (let [new-db (:db-after
                 @(transact [[:db/add [:user.registered/keycloak-id keycloak-id]
                              field value]]))]
     (fast-pull [:user.registered/keycloak-id keycloak-id] pattern new-db))))

(>defn update-display-name
  "Update the name of an existing user."
  [keycloak-id display-name]
  [:user.registered/keycloak-id string? :ret ::specs/registered-user]
  (add-user-field keycloak-id :user.registered/display-name display-name))

(>defn update-profile-picture-url
  "Update the profile picture url."
  [keycloak-id profile-picture-url]
  [:user.registered/keycloak-id :user.registered/profile-picture :ret ::specs/registered-user]
  (add-user-field keycloak-id :user.registered/profile-picture profile-picture-url))

(>defn members-of-group
  "Returns all members of a certain group."
  [group-name]
  [::specs/non-blank-string :ret (s/coll-of ::specs/any-user)]
  (query
   '[:find [(pull ?users public-user-pattern) ...]
     :in $ ?group public-user-pattern
     :where [?users :user.registered/groups ?group]]
   group-name patterns/public-user))

;; -----------------------------------------------------------------------------
;; Role Management

(>defn add-role
  "Add a role to a user."
  [keycloak-id role]
  [:user.registered/keycloak-id :user.registered/valid-roles => ::specs/registered-user]
  (add-user-field keycloak-id :user.registered/roles role patterns/private-user))

(>defn remove-role
  "Remove a role from a user." [keycloak-id role]
  [:user.registered/keycloak-id :user.registered/valid-roles => ::specs/registered-user]
  (let [new-db (:db-after
                @(transact [[:db/retract [:user.registered/keycloak-id keycloak-id]
                             :user.registered/roles role]]))]
    (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user new-db)))

;; -----------------------------------------------------------------------------

(>defn update-notification-mail-interval
  "Update the name of an existing user."
  [keycloak-id interval]
  [:user.registered/keycloak-id :user.registered/notification-mail-interval :ret ::specs/registered-user]
  (add-user-field keycloak-id :user.registered/notification-mail-interval interval patterns/private-user))

(>defn subscribed-share-hashes
  "Return all subscribed share-hashes from the users respecting their 
  notification settings."
  [interval]
  [:user.registered/notification-mail-interval :ret (s/coll-of :discussion/share-hash)]
  (->> (users-by-notification-interval interval)
       (map :user.registered/visited-schnaqs)
       flatten
       (map :discussion/share-hash)
       (remove nil?)
       set))

;; -----------------------------------------------------------------------------
;; Subscriptions

(>defn subscribe-pro-tier
  "Confirm subscription of pro tier and persist it in the user."
  [keycloak-id stripe-subscription-id stripe-customer-id]
  [:user.registered/keycloak-id :user.registered.subscription/stripe-id :user.registered.subscription/stripe-customer-id :ret ::specs/registered-user]
  (let [new-db (:db-after
                @(transact [{:db/id [:user.registered/keycloak-id keycloak-id]
                             :user.registered/roles :roles/pro
                             :user.registered.subscription/stripe-id stripe-subscription-id
                             :user.registered.subscription/stripe-customer-id stripe-customer-id}]))]
    (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user new-db)))

(>defn unsubscribe-pro-tier
  "Remove subscription from user."
  [keycloak-id]
  [:user.registered/keycloak-id :ret any?]
  (let [retractions [:db/retract [:user.registered/keycloak-id keycloak-id]]
        new-db (:db-after
                @(transact [(conj retractions :user.registered.subscription/type)
                            (conj retractions :user.registered.subscription/stripe-id)
                            (conj retractions :user.registered.subscription/stripe-customer-id)]))]
    (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user new-db)))

(>defn pro-subscription?
  "Check in our database the pro-subscription status of the user."
  [keycloak-id]
  [:user.registered/keycloak-id :ret boolean?]
  (-> (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user)
      :user.registered.subscription/type
      :db/ident
      (= :user.registered.subscription.type/pro)))

(def keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18")

(comment
  
  (:user.registered/roles (fast-pull [:user.registered/keycloak-id keycloak-id] patterns/private-user))

  nil)
