(ns schnaq.meeting.models)

(def datomic-schema
  [;; User
   {:db/ident :user/nickname
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/value
    :db/doc "User is identified by the nickname, when using the site without an account."}
   {:db/ident :user/upvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All upvotes the user gave."}
   {:db/ident :user/downvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All downvotes the user gave."}

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

   ;; Statement
   {:db/ident :statement/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of the statement"}
   {:db/ident :statement/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The text-content of the statement"}
   {:db/ident :statement/version
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The version of the statement. Always positive"}
   {:db/ident :statement/deleted?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "A marker whether the statement has been marked as deleted."}

   ;; Argument Types
   {:db/ident :argument.type/support}
   {:db/ident :argument.type/attack}
   {:db/ident :argument.type/undercut}
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
   {:db/ident :discussion.state/public}

   ;; Discussion
   {:db/ident :discussion/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The title / heading of a discussion. This should be system-widely unique."}
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
    :db/doc "The statements at the source of the discussion-graph"}])
