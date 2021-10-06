(ns schnaq.database.patterns
  "Define pull patterns for the database.")

(declare access-code-pattern)

(def registered-user-public-pattern
  "Small version of a user to show only necessary information."
  [:db/id
   :user.registered/keycloak-id
   :user.registered/display-name
   :user.registered/profile-picture])

(def registered-private-user-pattern
  [:user.registered/email
   :user.registered/last-name
   :user.registered/first-name
   {:user.registered/notification-mail-interval [:db/ident]}
   {:user.registered/visited-schnaqs [:discussion/share-hash]}])

(def seen-statements-pattern
  [:seen-statements/user
   :seen-statements/visited-schnaq
   {:seen-statements/visited-statements [:db/id]}])

(def ^:private minimal-user-pattern
  "Minimal user pull pattern."
  [:db/id
   :user/nickname])

(def public-user-pattern
  "Use this pattern to query public user information."
  (concat registered-user-public-pattern minimal-user-pattern))

(def private-user-pattern
  "When all data is necessary for a user, use this pattern."
  (concat public-user-pattern registered-private-user-pattern))


;; -----------------------------------------------------------------------------

(def statement-pattern
  "Representation of a statement. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :statement/content
   :statement/version
   :statement/deleted?
   :statement/created-at
   :statement/parent
   :statement/labels
   :statement/upvotes
   :statement/downvotes
   {:statement/type [:db/ident]}
   {:statement/author public-user-pattern}])

(def statement-pattern-with-secret
  (conj statement-pattern :statement/creation-secret))


;; -----------------------------------------------------------------------------

(def discussion-pattern
  "Representation of a discussion. Oftentimes used in a Datalog pull pattern."
  [:db/id
   :discussion/title
   :discussion/description
   {:discussion/states [:db/ident]}
   {:discussion/starting-statements statement-pattern}
   :discussion/share-hash
   :discussion/header-image-url
   {:discussion/mode [:db/ident]}
   :discussion/created-at
   :discussion/end-time
   {:discussion/author public-user-pattern}
   {[:discussion.access/_discussion :as :discussion.access/discussion]
    access-code-pattern}])

(def discussion-pattern-private
  "Holds sensitive information as well."
  (conj discussion-pattern :discussion/edit-hash))

(def discussion-pattern-minimal
  [:db/id
   :discussion/title
   {:discussion/states [:db/ident]}
   :discussion/share-hash
   :discussion/header-image-url
   :discussion/created-at
   {:discussion/author public-user-pattern}])

(def access-code-pattern
  "Return the access code for a discussion."
  [:db/id
   :discussion.access/code
   {:discussion.access/discussion discussion-pattern}
   :discussion.access/created-at
   :discussion.access/expires-at])
