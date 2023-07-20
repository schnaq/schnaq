(ns schnaq.database.specs
  (:require #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.string :as string]
            [schnaq.config.shared :as shared-config])
  #?(:clj (:import [java.io InputStream]
                   [java.util.regex Pattern])))

#?(:cljs (s/def :re-frame/component (s/or :form-1 vector? :form-2 fn?)))

#?(:clj (s/def ::regex (partial instance? Pattern)))

;; Common
(s/def ::non-blank-string (s/and string? (complement string/blank?)))
(s/def ::keyword-or-string (s/or :keyword keyword? :string string?))
(s/def ::component-or-string (s/or :component :re-frame/component :string string?))

(s/def :db/id (s/or :transacted integer? :temporary any?))
(s/def :db/txInstant inst?)
(s/def :color/hex (s/and ::non-blank-string #(.startsWith % "#") #(= 7 (.length %))))
(s/def ::email (s/and ::non-blank-string #(.contains % "@") #(.contains % ".")))

(def uuid-pattern
  #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
(s/def ::uuid-str (s/and string? #(re-matches uuid-pattern %)))

;; API
(s/def :api.response/error keyword?)
(s/def :api.response/message string?)
(s/def :api.response/error-body
  (s/keys :req-un [:api.response/error :api.response/message]))

;; Stripe
(s/def :stripe/customer-id (s/and string? #(.startsWith % "cus_")))

(s/def :stripe.price/cost number?)
(s/def :stripe.price/id (s/and string? #(.startsWith % "price_")))
(s/def :stripe.price/interval #{:month :year})
(s/def :stripe/price
  (s/or :valid (s/keys :req-un [:stripe.price/id :stripe.price/cost :stripe.price/interval])
        :request-failed :api.response/error-body))
(s/def :stripe/kw-to-price (s/map-of keyword? :stripe/price))
(s/def :stripe/prices (s/map-of keyword? :stripe/kw-to-price))

(s/def :stripe.subscription/id (s/and #(.startsWith % "sub_") string?))
(s/def :stripe.subscription/status #{:incomplete :incomplete_expired :trialing :active :past_due :canceled :unpaid})
(s/def :stripe.subscription/cancelled? boolean?)
(s/def :stripe.subscription/period-start nat-int?)
(s/def :stripe.subscription/period-end nat-int?)
(s/def :stripe.subscription/cancel-at nat-int?)
(s/def :stripe.subscription/cancelled-at nat-int?)
(s/def :stripe/subscription
  (s/keys :req-un [:stripe.subscription/status :stripe.subscription/cancelled?
                   :stripe.subscription/period-start :stripe.subscription/period-end]
          :opt-un [:stripe.subscription/cancel-at :stripe.subscription/cancelled-at]))

;; User
(s/def :user/nickname string?)
(s/def ::user (s/keys :opt [:user/nickname]))

;; Registered user
(s/def :user.registered/keycloak-id ::uuid-str)
(s/def :user.registered/email ::non-blank-string)
(s/def :user.registered/display-name ::non-blank-string)
(s/def :user.registered/first-name ::non-blank-string)
(s/def :user.registered/last-name ::non-blank-string)
(s/def :user.registered/profile-picture ::non-blank-string)
(s/def :user.registered/notification-mail-interval #{:notification-mail-interval/daily
                                                     :notification-mail-interval/weekly
                                                     :notification-mail-interval/every-minute
                                                     :notification-mail-interval/never})
(def user-roles #{:role/admin :role/enterprise :role/tester :role/pro :role/analytics})
(s/def :user.registered/valid-roles user-roles)
(s/def :user.registered/roles
  (s/or :coll (s/coll-of :user.registered/valid-roles
                         :distinct true)
        :role :user.registered/valid-roles))
(s/def :user.registered/groups (s/coll-of ::non-blank-string))
(s/def :user.registered/visited-schnaqs (s/or :ids (s/coll-of :db/id)
                                              :schnaqs (s/coll-of ::discussion)))

(s/def :user.registered.subscription/stripe-id :stripe.subscription/id)
(s/def :user.registered.subscription/stripe-customer-id :stripe/customer-id)

(s/def :user.registered.features/concurrent-users nat-int?)
(s/def :user.registered.features/total-schnaqs nat-int?)
(s/def :user.registered.features/posts-per-schnaq nat-int?)

(s/def ::registered-user (s/keys :opt [:user.registered/keycloak-id
                                       :user.registered/display-name
                                       :user.registered/last-name :user.registered/first-name
                                       :user.registered/groups :user.registered/profile-picture
                                       :user.registered/roles
                                       :user.registered/email :user.registered/notification-mail-interval
                                       :user.registered/visited-schnaqs
                                       :user.registered.subscription/stripe-id
                                       :user.registered.subscription/stripe-customer-id
                                       :user.registered.features/concurrent-users
                                       :user.registered.features/total-schnaqs
                                       :user.registered.features/posts-per-schnaq]))

;; Could be anonymous or registered
(s/def ::any-user (s/or :user ::user
                        :registered-user ::registered-user))
;; Any user or reference
(s/def ::user-or-reference
  (s/or :user ::user
        :reference :db/id
        :registered-user ::registered-user))

;; Meta Information
(s/def :meta/all-statements nat-int?)
;; Marks whether the user has up / downvoted a statement themselves.
(s/def :meta/upvoted? boolean?)
(s/def :meta/downvoted? boolean?)
(s/def :meta/authors (s/coll-of :user/nickname))
(s/def :meta/sub-statement-count number?)

;; Access Codes
(s/def :discussion.access/code
  (s/and nat-int?
         #(< % (Math/pow 10 shared-config/access-code-length))))
(s/def :discussion.access/discussion :db/id)
(s/def :discussion.access/created-at inst?)
(s/def :discussion.access/expires-at inst?)
(s/def ::access-code-template
  (s/keys :req [:discussion.access/code]
          :opt [:discussion.access/discussion
                :discussion.access/created-at :discussion.access/expires-at]))
(s/def :discussion/access
  (s/or :from-db (s/coll-of ::access-code-template)
        :regular ::access-code-template))

;; Discussion
(s/def :discussion/title string?)
(s/def :discussion/description string?)
(s/def :discussion/share-hash ::non-blank-string)
(s/def :discussion/share-link ::non-blank-string)
(s/def :discussion/moderation-link ::non-blank-string)
(s/def :discussion/created-at inst?)
(s/def :discussion/author ::user-or-reference)
(s/def :discussion/header-image-url string?)
(s/def :discussion/device-ids (s/coll-of uuid?))
(s/def :discussion/hub-origin (s/or :reference :db/id
                                    :hub ::hub))
(s/def :discussion/moderators (s/coll-of (s/or :registered-user ::registered-user
                                               :reference :db/id)))
(s/def :discussion/creation-secret ::non-blank-string)
(s/def :discussion/activation-focus :db/id)
(s/def :discussion/valid-states #{:discussion.state/open :discussion.state/closed
                                  :discussion.state/private :discussion.state/deleted
                                  :discussion.state/public :discussion.state/read-only
                                  :discussion.state/disable-pro-con :discussion.state/disable-posts
                                  :discussion.state.qa/mark-as-moderators-only})
(s/def :discussion/states
  (s/coll-of :discussion/valid-states
             :distinct true))
(s/def :discussion/mode #{:discussion.mode/discussion :discussion.mode/qanda})
(s/def :wordcloud/visible? boolean?)
(s/def :discussion/wordcloud
  (s/keys :req [:db/id :wordcloud/visible?]))
(s/def :discussion/starting-statements (s/coll-of ::statement))
(s/def :discussion/qa-box ::qa-box)
(s/def ::discussion (s/keys :req [:discussion/share-hash]
                            :opt [:discussion/title :discussion/author
                                  :discussion/starting-statements :discussion/description
                                  :discussion/header-image-url :discussion/hub-origin
                                  :discussion/moderators :discussion/states
                                  :discussion/created-at :discussion/share-link :discussion/moderation-link
                                  :discussion/creation-secret :discussion/mode :discussion/access
                                  :discussion/activation-focus :discussion/wordcloud
                                  :discussion/qa-box
                                  :discussion/device-ids :discussion/feedback]))

(s/def :feedback.item/label ::non-blank-string)
(s/def :feedback.item/ordinal pos-int?)
(s/def :feedback.item/type #{:feedback.item.type/text :feedback.item.type/scale-five})
(s/def ::feedback-item (s/keys :req [:feedback.item/label :feedback.item/ordinal :feedback.item/type]
                               :opt [:db/id]))
(s/def :feedback/items (s/coll-of ::feedback-item))
(s/def :feedback.answer/item (s/or :id :db/id
                                   :item ::feedback-item))

(s/def :feedback.answer/text ::non-blank-string)
(s/def :feedback.answer/scale-five (s/and pos-int? #(<= % 5)))
(s/def ::feedback-answer (s/keys :opt [:feedback.answer/text :feedback.answer/scale-five :feedback.answer/item :db/id]))
(s/def :feedback/answers (s/coll-of ::feedback-answer))
(s/def :feedback/visible boolean?)
(s/def ::feedback-form (s/keys :req [:feedback/items]
                               :opt [:feedback/answers :feedback/visible]))
(s/def :discussion/feedback (s/or :id :db/id
                                  :feedback-form ::feedback-form))

(s/def :wordcloud/title ::non-blank-string)
(s/def :wordcloud/discussion (s/or :id :db/id
                                   :discussion ::discussion))
(s/def :wordcloud/words (s/coll-of (s/tuple ::non-blank-string pos-int?)))
(s/def ::wordcloud (s/keys :req [:wordcloud/title :wordcloud/discussion]
                           :opt [:wordcloud/words]))

(s/def ::share-hash-statement-id-mapping
  (s/map-of :discussion/share-hash (s/coll-of :db/id)))

;; Hubs
(s/def :hub/name ::non-blank-string)
(s/def :hub/keycloak-name ::non-blank-string)
(s/def :hub/logo ::non-blank-string)
(s/def :hub/schnaqs (s/coll-of ::discussion))
(s/def :hub/created-at inst?)
(s/def ::hub (s/keys :req [:hub/name :hub/keycloak-name]
                     :opt [:hub/logo :hub/schnaqs :hub/created-at]))
;; image
(s/def :file/type string?)
(s/def :file/name string?)
(s/def :file/content string?)
(s/def :file/size number?)
(s/def ::file
  (s/keys :req-un [:file/content]
          :opt-un [:file/type :file/name :file/size]))
(s/def ::image ::file)

(s/def :file-stored/url string?)
(s/def :file-stored/error keyword?)
(s/def :file-stored/message string?)
(s/def ::file-stored
  (s/keys :opt-un [:file-stored/url :file-stored/error :file-stored/message]))
#?(:clj (s/def :type/input-stream (partial instance? InputStream)))

;; Statement
(s/def :statement/type #{:statement.type/attack :statement.type/support :statement.type/neutral})
(s/def :statement/parent (s/or :id :db/id :statement ::statement))
(s/def :statement/content ::non-blank-string)
(s/def :statement/version number?)
(s/def :statement/author ::any-user)
(s/def :statement/upvotes (s/or :count number? :upvote-users (s/coll-of ::user-or-reference)))
(s/def :statement/cumulative-upvotes nat-int?)
(s/def :statement/cumulative-downvotes nat-int?)
(s/def :statement/downvotes (s/or :count number? :downvote-users (s/coll-of ::user-or-reference)))
(s/def :statement/creation-secret ::non-blank-string)
(s/def :statement/created-at inst?)
(s/def :statement/label shared-config/allowed-labels)
(s/def :statement/labels (s/coll-of :statement/label))
(s/def :statement/locked? boolean?)
(s/def :statement/pinned? boolean?)
(s/def :statement/discussions (s/or :ids (s/coll-of :db/id)
                                    :discussions (s/coll-of ::discussion)))
(s/def ::statement
  (s/keys :req [:statement/content :statement/version :statement/author]
          :opt [:statement/creation-secret :statement/created-at
                :statement/type :statement/parent :statement/discussions :statement/pinned?
                :statement/labels :statement/cumulative-upvotes :statement/cumulative-downvotes :statement/locked?]))

(s/def :statement.vote/operation #{:removed :switched :added :succeeded})

;; Feedback
(s/def :feedback/contact-name string?)
(s/def :feedback/contact-mail string?)
(s/def :feedback/description ::non-blank-string)
(s/def :feedback/has-image? boolean?)
(s/def :feedback/created-at inst?)
(s/def :feedback/screenshot ::non-blank-string)
(s/def ::feedback (s/keys :req [:feedback/description :feedback/has-image?]
                          :opt [:feedback/contact-name :feedback/contact-mail
                                :feedback/created-at :feedback/screenshot]))

;; Summary
(s/def :summary/discussion (s/or :id :db/id
                                 :discussion (s/keys :req [:discussion/title
                                                           :discussion/share-hash
                                                           :db/id])))
(s/def :summary/requested-at inst?)
(s/def :summary/created-at inst?)
(s/def :summary/text ::non-blank-string)
(s/def :summary/requester (s/or :id :db/id
                                :registered-user (s/keys :req [:user.registered/email
                                                               :user.registered/display-name
                                                               :user.registered/keycloak-id])))
(s/def ::summary (s/keys :req [:summary/discussion :summary/requested-at]
                         :opt [:summary/text :summary/created-at :summary/requester]))

;; Graph
(s/def :node/id (s/or :share-hash :discussion/share-hash
                      :id :db/id))
(s/def :node/label string?)
(s/def :node/author string?)
(s/def :node/type #{:statement.type/starting :statement.type/support :statement.type/attack
                    :statement.type/neutral :agenda})
(s/def :graph/node (s/keys :req-un [:node/id :node/label :node/author :node/type]))

(s/def :edge/from :node/id)
(s/def :edge/to :node/id)
(s/def :edge/type :node/type)
(s/def :graph/edge (s/keys :req-un [:edge/from :edge/to :edge/type]))

(s/def :graph/nodes (s/coll-of :graph/node))
(s/def :graph/edges (s/coll-of :graph/edge))
(s/def :graph/controversy-values associative?)
(s/def ::graph (s/keys :req-un [:graph/nodes :graph/edges :graph/controversy-values]))

;; Statistics
(s/def :statistics/since inst?)
(s/def :statistics/discussions-sum nat-int?)
(s/def :statistics/usernames-sum nat-int?)
(s/def :statistics/average-statements-num number?)
(s/def :statistics/statements-num map?)
(s/def :statistics/active-users-num map?)
(s/def :statistics.statement.type/supports nat-int?)
(s/def :statistics.statement.type/attacks nat-int?)
(s/def :statistics.statement.type/neutrals nat-int?)
(s/def :statistics/statement-type-stats
  (s/keys :req-un [:statistics.statement.type/supports :statistics.statement.type/attacks
                   :statistics.statement.type/neutrals]))
(s/def :statistics.statement.length/max number?)
(s/def :statistics.statement.length/min number?)
(s/def :statistics.statement.length/average number?)
(s/def :statistics.statement.length/median number?)
(s/def :statistics/statement-length-stats
  (s/keys :req-un [:statistics.statement.length/max :statistics.statement.length/min
                   :statistics.statement.length/average :statistics.statement.length/median]))
(s/def :statistics/registered-users-num nat-int?)
(s/def :statistics/labels-stats map?)
(s/def :statistics/statement-percentiles map?)
(s/def :statistics/users (s/coll-of ::registered-user))
(s/def :statistics/usage (s/coll-of (s/tuple keyword? nat-int?)))

(s/def ::statistics
  (s/keys :req-un [:statistics/discussions-sum :statistics/usernames-sum
                   :statistics/average-statements-num :statistics/statements-num
                   :statistics/active-users-num :statistics/statement-length-stats
                   :statistics/statement-type-stats :statistics/registered-users-num
                   :statistics/labels-stats :statistics/users :statistics/statement-percentiles
                   :statistics/usage]))

;; Polls
(s/def :poll/title ::non-blank-string)
(s/def :poll/type #{:poll.type/multiple-choice :poll.type/single-choice :poll.type/ranking})
(s/def :poll/discussion (s/or :id :db/id
                              :discussion ::discussion))
(s/def :option/value ::non-blank-string)
(s/def :option/votes nat-int?)
(s/def ::option (s/keys :req [:option/value]
                        :opt [:option/votes]))
(s/def :poll/options (s/coll-of ::option))
(s/def :poll/hide-results? boolean?)
(s/def ::poll
  (s/keys :req [:poll/title :poll/options :poll/type :poll/discussion]
          :opt [:poll/hide-results?]))

;; Question Box
(s/def :qa-box/label ::non-blank-string)
(s/def :qa-box/visible boolean?)
(s/def :qa-box.question/answered boolean?)
(s/def :qa-box.question/value ::non-blank-string)
(s/def :qa-box.question/upvotes nat-int?)
(s/def :qa-box/question (s/keys :req-un [:qa-box.question/value :qa-box.question/answered]
                                :opt-un [:qa-box.question/upvotes]))
(s/def :qa-box/questions (s/coll-of :qa-box/question))
(s/def ::qa-box
  (s/keys :req [:qa-box/visible :qa-box/questions]
          :opt [:qa-box/label]))

;; Activation
(s/def :activation/discussion (s/or :id :db/id :discussion ::discussion))
(s/def :activation/count nat-int?)
(s/def ::activation (s/keys :req [:db/id
                                  :activation/count
                                  :activation/discussion]))

;; App-Codes
(s/def :app/code ::non-blank-string)

;; HTTP Related
(s/def :http/status nat-int?)
(s/def :http/headers map?)
(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))
(s/def :ring/body-params map?)
(s/def :ring/route-params map?)
(s/def :ring/request (s/keys :opt [:ring/body-params :ring/route-params]))

;; Theming
(s/def :theme/title ::non-blank-string)
(s/def :theme/user :db/id)
(s/def :theme/discussions (s/coll-of :db/id))
(s/def :theme.colors/primary :color/hex)
(s/def :theme.colors/secondary :color/hex)
(s/def :theme.colors/background :color/hex)
(s/def :theme.images/logo ::non-blank-string)
(s/def :theme.images/header ::non-blank-string)
(s/def :theme.images.raw/logo ::image)
(s/def :theme.images.raw/header ::image)
(s/def :theme.texts/activation ::non-blank-string)
(s/def ::theme
  (s/keys :opt [:theme/title
                :theme.colors/primary :theme.colors/secondary
                :theme.colors/background :theme.images/logo
                :theme.images/header :theme/discussions
                :theme.texts/activation :theme/user :db/id
                :theme.images.raw/logo :theme.images.raw/header]))

;; User's identity parsed from a JWT token
;; There are plenty of more fields, but let's for now spec those we really need.
(s/def :identity/sub :user.registered/keycloak-id)
(s/def :identity/given_name string?)
(s/def :identity/family_name string?)
(s/def :identity/preferred_username string?)
(s/def :identity/email ::email)
(s/def :identity/groups (s/coll-of string?))
(s/def :identity/locale string?)
(s/def ::identity
  (s/keys
   :opt-un [:identity/sub :identity/preferred_username :identity/email
            :identity/given_name :identity/family_name :identity/groups :identity/locale]))

;; -----------------------------------------------------------------------------
;; Websockets
(s/def :ws.message/?reply-fn fn?) ;; Optional callable function after event succeeds
(s/def :ws.message/?data any?) ;; payload from the event
(s/def :ws.message/ch-recv any?) ;; an async channel
(s/def :ws.message/client-id string?) ;; uuid
(s/def :ws.message/connected-uids any?) ;; atom with keys :ws, :ajax, :any
(s/def :ws.message/uid string?) ;; uuid
(s/def :ws.message/event vector?) ;; complete event with id, ?data and ?reply-fn
(s/def :ws.message/id keyword?) ;; event-id
(s/def :ws.message/send-buffers any?) ;; event-id
(s/def :ws.message/ring-req map?) ;; complete ring request
(s/def :ws.message/send-fn fn?) ;; fn to be called from the websocket library
(s/def ::websocket-message
  (s/keys
   :req-un [:ws.message/ch-recv :ws.message/client-id :ws.message/connected-uids
            :ws.message/uid :ws.message/event :ws.message/id
            :ws.message/send-buffers :ws.message/ring-req :ws.message/send-fn]
   :opt-un [:ws.message/?reply-fn :ws.message/?data]))

;; -----------------------------------------------------------------------------
;; Surveys

(s/def :surveys.using-schnaq-for/user ::user-or-reference)
(s/def :surveys.using-schnaq-for/topics
  (s/coll-of #{:surveys.using-schnaq-for.topics/education
               :surveys.using-schnaq-for.topics/coachings
               :surveys.using-schnaq-for.topics/seminars
               :surveys.using-schnaq-for.topics/fairs
               :surveys.using-schnaq-for.topics/meetings
               :surveys.using-schnaq-for.topics/other}))
(s/def :surveys/using-schnaq-for
  (s/keys :req [:surveys.using-schnaq-for/user
                :surveys.using-schnaq-for/topics]
          :opt [:db/id]))

;; -----------------------------------------------------------------------------
;; UI Settings

(s/def :ui.settings/hide-discussion-options boolean?)
(s/def :ui.settings/hide-navbar boolean?)
(s/def :ui.settings/hide-footer boolean?)
(s/def :ui.settings/hide-input boolean?)
(s/def :ui.settings/hide-input-replies boolean?)
(s/def :ui.settings/hide-activations boolean?)
(s/def :ui.settings/num-rows nat-int?)

(s/def :ui.settings/schnaq
  (s/keys :opt-un [:ui.settings/hide-discussion-options
                   :ui.settings/hide-navbar
                   :ui.settings/hide-footer
                   :ui.settings/hide-input
                   :ui.settings/hide-input-replies
                   :ui.settings/hide-activations
                   :ui.settings/num-rows]))

;; -----------------------------------------------------------------------------
;; Feature limits

(s/def ::feature-limits
  #{:wordcloud? :activation? :rankings? :polls :theming? :embeddings? :concurrent-users :total-schnaqs :posts-per-schnaq})

(s/def ::warning-levels #{:warning :danger})
