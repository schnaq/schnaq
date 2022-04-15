(ns schnaq.database.patterns
  "Define pull patterns for the database.")

(def ^:private registered-user-public
  "Small version of a user to show only necessary information."
  [:db/id
   :user.registered/keycloak-id
   :user.registered/display-name
   :user.registered/profile-picture])

(def ^:private registered-private-user
  [:user.registered/email
   :user.registered/last-name
   :user.registered/first-name
   {[:user.registered/notification-mail-interval :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   {:user.registered/visited-schnaqs [:discussion/share-hash]}
   {[:user.registered.subscription/type :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   :user.registered.subscription/stripe-id
   :user.registered.subscription/stripe-customer-id])

(def seen-statements
  [:seen-statements/user
   :seen-statements/visited-schnaq
   {:seen-statements/visited-statements [:db/id]}])

(def ^:private minimal-user
  "Minimal user pull pattern."
  [:db/id
   :user/nickname])

(def public-user
  "Use this pattern to query public user information."
  (concat registered-user-public minimal-user))

(def private-user
  "When all data is necessary for a user, use this pattern."
  (concat public-user registered-private-user))

;; -----------------------------------------------------------------------------

(def statement
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
   :statement/locked?
   :statement/pinned?
   :statement/cumulative-downvotes
   :statement/cumulative-upvotes
   [:statement/_parent :as :statement/children :xform 'schnaq.database.xforms/maps->ids]
   {[:statement/type :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   {:statement/author public-user}])

(def statement-with-secret
  (conj statement :statement/creation-secret))

;; -----------------------------------------------------------------------------

(def access-code
  "Return the access code for a discussion."
  [:db/id
   :discussion.access/code
   :discussion.access/created-at
   :discussion.access/expires-at])

(def theme
  [:db/id
   :theme/title
   :theme/user
   :theme/discussions
   :theme.colors/primary
   :theme.colors/secondary
   :theme.colors/background
   :theme.images/logo
   :theme.images/header
   :theme.texts/activation])

(def discussion-minimal
  [:db/id
   :discussion/title
   {[:discussion/states :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   :discussion/share-hash
   :discussion/header-image-url
   :discussion/created-at
   {:discussion/author public-user}
   {[:discussion.visible/entities :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   {:discussion/theme theme}])

(def discussion
  "Representation of a discussion."
  (concat
   discussion-minimal
   [:discussion/description
    {:discussion/starting-statements statement}
    {[:discussion/mode :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
    {[:discussion.access/_discussion :as :discussion/access] access-code}]))

(def discussion-private
  "Holds sensitive information as well."
  (conj discussion :discussion/edit-hash))

(def access-code-with-discussion
  "Return the access-code and directly query the discussion."
  (conj access-code {:discussion.access/discussion discussion}))

;; -----------------------------------------------------------------------------

(def ^:private minimal-summary
  [:db/id
   :summary/requested-at
   :summary/text
   :summary/created-at])

(def summary
  (conj minimal-summary :summary/discussion))

(def summary-with-discussion
  (conj
   minimal-summary
   {:summary/discussion [:discussion/title
                         :discussion/share-hash
                         :db/id]}
   {:summary/requester [:user.registered/email
                        :user.registered/display-name
                        :user.registered/keycloak-id]}))

(def poll
  [:db/id
   :poll/title
   {[:poll/type :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}
   {:poll/options [:db/id
                   :option/value
                   [:option/votes :default 0]]}
   {:poll/discussion [:db/id
                      :discussion/share-hash
                      :discussion/title]}])

(def activation
  [:db/id
   :activation/count
   {:activation/discussion [:db/id
                            :discussion/share-hash
                            :discussion/title]}])

(def survey-using-schnaq-for
  [:db/id
   :surveys.using-schnaq-for/user
   {[:surveys.using-schnaq-for/topics :xform 'schnaq.database.xforms/pull-up-db-ident] [:db/ident]}])
