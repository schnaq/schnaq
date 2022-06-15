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
   {:db/ident :user.registered/visited-schnaqs
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The schnaqs that are known to the registered user, i.e. they can open and view."}
   {:db/ident :user.registered/archived-schnaqs
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Archived schnaqs of a user."}
   {:db/ident :user.registered/notification-mail-interval
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Interval in which the user will receive updates via mail"}

   ;; Notification Mail Intervals
   {:db/ident :notification-mail-interval/every-minute}
   {:db/ident :notification-mail-interval/daily}
   {:db/ident :notification-mail-interval/weekly}
   {:db/ident :notification-mail-interval/never}

   ;; Subscriptions
   {:db/ident :user.registered.subscription/stripe-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Store the subscription id of stripe."}
   {:db/ident :user.registered.subscription/stripe-customer-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Store the customer id from stripe."}
   {:db/ident :user.registered.subscription/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Define which plan the user subscribed to."}
   {:db/ident :user.registered.subscription.type/pro}

   ;; Seen statements
   {:db/ident :seen-statements/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The keycloak id of a registered user."}
   {:db/ident :seen-statements/visited-schnaq
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "A visited schnaq to reference a set of visited statements to."}
   {:db/ident :seen-statements/visited-statements
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The statements that are known to the registered user, i.e. visited statements."}

   ;; Surveys
   {:db/ident :surveys.using-schnaq-for/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The keycloak id of a registered user."}
   {:db/ident :surveys.using-schnaq-for/topics
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "Reference the selections of the user."}
   {:db/ident :surveys.using-schnaq-for.topics/education}
   {:db/ident :surveys.using-schnaq-for.topics/coachings}
   {:db/ident :surveys.using-schnaq-for.topics/seminars}
   {:db/ident :surveys.using-schnaq-for.topics/fairs}
   {:db/ident :surveys.using-schnaq-for.topics/meetings}
   {:db/ident :surveys.using-schnaq-for.topics/other}

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
    :db/doc "A list of registered users that upvoted the statement."}
   {:db/ident :statement/downvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "A list of registered users that downvoted the statement."}
   {:db/ident :statement/cumulative-upvotes
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The cumulative number of upvotes by anonymous users."}
   {:db/ident :statement/cumulative-downvotes
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The cumulative number of downvotes by anonymous users."}
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
   {:db/ident :statement/discussions
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "In which discussions is this statement used?"}
   {:db/ident :statement/labels
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many
    :db/doc "A label that is assigned to the statement."}
   {:db/ident :statement/locked?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Indicates whether the statement should be locked (= not allow child-statements)."}
   {:db/ident :statement/pinned?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Indicates whether the statement is pinned (at the top of the schnaq)."}

   ;; Statement Types
   {:db/ident :statement.type/support}
   {:db/ident :statement.type/attack}
   {:db/ident :statement.type/neutral}

   ;; Discussion States
   {:db/ident :discussion.state/open
    :db/doc "DEPRECATED. Not in use anymore. In db for legacy reasons. Discussions are open unless the have the closed state."}
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
   {:db/ident :discussion.state.qa/mark-as-moderators-only
    :db/doc "When set, only moderators are allowed to mark a statement as an answer."}

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
   {:db/ident :discussion/end-time
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "An optional time, when the discussion is finished and follow-up processes are started.
    As of 2021.12.13 not in use."}
   {:db/ident :discussion/creation-secret
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A secret to claim ownership of a discussion as an anonymous user."}
   {:db/ident :discussion/mode
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Define the mode of your discussion."}
   {:db/ident :discussion/activation-focus
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The activation element which should be displayed in focus. E.g. first element in the activations card."}

   {:db/ident :discussion.mode/qanda
    :db/doc "Q&A mode."}
   {:db/ident :discussion.mode/discussion
    :db/doc "Discussion mode."}

   {:db/ident :discussion.access/code
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "Generated access code for a discussion."}
   {:db/ident :discussion.access/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to a discussion."}
   {:db/ident :discussion.access/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp when access code was generated"}
   {:db/ident :discussion.access/expires-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Timestamp indicating when the access code becomes invalid."}

   {:db/ident :discussion/theme
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The discussion's theme."}

   ;; hub
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
    :db/doc "The time at which this entity has been created."}
   {:db/ident :hub/logo
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "URL to where the hub logo was uploaded to."}

   ;; Extractive summaries
   {:db/ident :summary/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The discussion which is summarized."}
   {:db/ident :summary/requested-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When was this summary requested?"}
   {:db/ident :summary/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When has the summary been successfully created. Can be empty when there is no summary yet. Represents the creation time of the newest summary for that discussion."}
   {:db/ident :summary/text
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The most current summary, if there is one."}
   {:db/ident :summary/requester
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The last requester (registered user) of a summary."}

   ;; Polls
   {:db/ident :poll/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The title of a poll. Usually a question that is posed."}
   {:db/ident :poll/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The discussion that the poll belongs to."}
   {:db/ident :poll/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The type of poll being conducted. i.e. multiple or single choice. Referenced by `:poll.type` entities"}
   {:db/ident :poll.type/single-choice
    :db/doc "A typical single-choice poll, where only one vote per person is allowed."}
   {:db/ident :poll.type/multiple-choice
    :db/doc "A multiple choice poll, where participants may choose multiple answers."}
   {:db/ident :poll.type/ranking
    :db/doc "A type of poll where the options are ranked. The first option gets the most points and the last option the least."}
   {:db/ident :poll/options
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "The options that are possible in this poll. An option knows how many voted for it."}
   {:db/ident :poll/hide-results?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Show or hide results to participants."}

   ;; Polls Option
   {:db/ident :option/value
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The option description, that the user gets to see. E.g. \"Milk\""}
   {:db/ident :option/votes
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "The cumulative number of votes for this option. Must be 0 or positive."}

   ;; Activation
   {:db/ident :activation/count
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Activation counter"}
   {:db/ident :activation/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The discussion to which the activation belongs to."}

   ;; Wordcloud
   {:db/ident :discussion/wordcloud
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/doc "Wordcloud configuration for a discussion."}
   {:db/ident :wordcloud/visible?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Hide or show the wordcloud."}

   ;; Themes
   {:db/ident :theme/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Title of a theme."}
   {:db/ident :theme/user
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to the creating user."}
   {:db/ident :theme.colors/primary
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Set the primary color of a theme."}
   {:db/ident :theme.colors/secondary
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Set the secondary color of a theme."}
   {:db/ident :theme.colors/background
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Set the background color of a theme."}
   {:db/ident :theme.images/logo
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The custom user logo."}
   {:db/ident :theme.images/header
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Header image for listings and activations."}
   {:db/ident :theme.texts/activation
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The user's activation message."}])
