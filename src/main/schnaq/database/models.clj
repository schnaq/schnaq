(ns schnaq.database.models)

(def datomic-schema
  [;; Anonymous User
   {:db/ident :user/nickname
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/doc "User is identified by the nickname, when using the site without an account."}

   ;; Registered Users. Their names are not unique, the keycloak-id is.
   {:db/ident :user.registered/keycloak-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/doc "The unique id that is given by keycloak to the user."}
   {:db/ident :user.registered/email
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/doc "The email of the user-account."}
   {:db/ident :user.registered/display-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The name that will be displayed in the frontend for the user."}
   {:db/ident :user.registered/first-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The first name."}
   {:db/ident :user.registered/last-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The first name."}
   {:db/ident :user.registered/groups
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "The groups the user is currently part of."}
   {:db/ident :user.registered/profile-picture
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "URL to where the profile picture was uploaded to."}

   ;; Feedback
   {:db/ident :feedback/contact-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Name of the person who gave feedback"}
   {:db/ident :feedback/contact-mail
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "How to contact the person who gave feedback"}
   {:db/ident :feedback/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The feedback description."}
   {:db/ident :feedback/has-image?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Indicate wether a user provided an image."}
   {:db/ident :feedback/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time at which this entity has been created."}

   ;; Statement
   {:db/ident :statement/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of the statement"}
   {:db/ident :statement/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/fulltext true
    :db/doc "The text-content of the statement"}
   {:db/ident :statement/version
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The version of the statement. Always positive"}
   {:db/ident :statement/deleted?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "A marker whether the statement has been marked as deleted."}
   {:db/ident :statement/upvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A list of users that upvoted the statement."}
   {:db/ident :statement/downvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A list of users that downvoted the statement."}
   {:db/ident :statement/creation-secret
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A secret to claim ownership of a statement as an anonymous user."}
   {:db/ident :statement/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time at which this entity has been created."}
   {:db/ident :statement/parent
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The parent statement which this statement is referencing (if any)."}
   {:db/ident :statement/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The type of the statement. (Is it supportive, attacking or neutral to the parent)"}

   ;; Statement Types
   {:db/ident :statement.type/support}
   {:db/ident :statement.type/attack}
   {:db/ident :statement.type/neutral}

   ;; Argument Types
   {:db/ident :argument.type/support}
   {:db/ident :argument.type/attack}
   {:db/ident :argument.type/undercut}
   {:db/ident :argument.type/neutral}

   ;; Argument
   {:db/ident :argument/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of an argument"}
   {:db/ident :argument/premises
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The premises of an argument constituting a premise-group."}
   {:db/ident :argument/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The type of the arguments edge"}
   {:db/ident :argument/conclusion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The conclusion of an argument"}
   {:db/ident :argument/version
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The version of an argument"}
   {:db/ident :argument/discussions
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The discussions in which the argument is used"}

   ;; Discussion States
   {:db/ident :discussion.state/open}
   {:db/ident :discussion.state/closed}
   {:db/ident :discussion.state/private
    :db/doc "DEPRECATED: Use :discussion.state/public"}
   {:db/ident :discussion.state/deleted
    :db/doc "Supersedes most other states. When set do absolutely not show under any circumstances"}
   {:db/ident :discussion.state/public
    :db/doc "Marks a discussion as publicly visible to everybody."}
   {:db/ident :discussion.state/read-only
    :db/doc "Marks a discussion as read-only. Only admins or nobody should be able to add something to the discussion."}
   {:db/ident :discussion.state/disable-pro-con
    :db/doc "Flag to disable the pro/con button."}

   ;; Discussion
   {:db/ident :discussion/title
    :db/valueType :db.type/string
    :db/fulltext true
    :db/cardinality :db.cardinality/one
    :db/doc "The title / heading of a discussion."}
   {:db/ident :discussion/share-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A unique hash that grants participation access to the discussion"}
   {:db/ident :discussion/edit-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A hash that grants edit access to the discussion"}
   {:db/ident :discussion/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of a meeting."}
   {:db/ident :discussion/admins
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The registered users, that are allowed to administrate the discussion."}
   {:db/ident :discussion/header-image-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "URL pointing to an image to be displayed as header."}
   {:db/ident :discussion/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The topic description of a discussion"}
   {:db/ident :discussion/states
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The states the discussion is in"}
   {:db/ident :discussion/starting-statements
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The statements at the source of the discussion-graph"}
   {:db/ident :discussion/hub-origin
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The hub in which the schnaq is originated in. The quasi owner of the schnaq."}
   {:db/ident :discussion/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time at which this entity has been created."}

   {:db/ident :hub/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The name of the hub."}
   {:db/ident :hub/schnaqs
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The schnaqs that are visible in the hub."}
   {:db/ident :hub/keycloak-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/doc "Map this entity to the group in our keycloak instance."}
   {:db/ident :hub/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time at which this entity has been created."}])
