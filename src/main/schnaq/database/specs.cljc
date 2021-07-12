(ns schnaq.database.specs
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.string :as string]))

(s/def ::non-blank-string (s/and string? (complement string/blank?)))

;; Frontend only
#?(:cljs (s/def :re-frame/component vector?))

;; Transaction
(s/def :db/txInstant inst?)

;; User
(s/def :user/nickname string?)
(s/def ::user (s/keys :req [:user/nickname]))

;; Registered user
(s/def :user.registered/keycloak-id ::non-blank-string)
(s/def :user.registered/email ::non-blank-string)
(s/def :user.registered/display-name ::non-blank-string)
(s/def :user.registered/first-name ::non-blank-string)
(s/def :user.registered/last-name ::non-blank-string)
(s/def :user.registered/profile-picture ::non-blank-string)
(s/def :user.registered/groups (s/coll-of ::non-blank-string))
(s/def :user.registered/visited-schnaqs (s/or :ids (s/coll-of :db/id)
                                              :schnaqs (s/coll-of ::discussion)))
(s/def ::registered-user (s/keys :req [:user.registered/keycloak-id :user.registered/display-name]
                                 :opt [:user.registered/last-name :user.registered/first-name
                                       :user.registered/groups :user.registered/profile-picture
                                       :user.registered/email :user.registered/visited-schnaqs]))

;; Could be anonymous or registered
(s/def ::any-user (s/or :user ::user :registered-user ::registered-user))
;; Any user or reference
(s/def ::user-or-reference (s/or :user ::user
                                 :reference ::entity-reference
                                 :registered-user ::registered-user))

;; Meta Information
(s/def :meta/all-statements nat-int?)
(s/def :meta/upvotes number?)
(s/def :meta/downvotes number?)
(s/def :meta/sub-statements number?)
(s/def :meta/authors (s/coll-of :user/nickname))
(s/def :meta/sub-discussion-info
  (s/keys :req-un [:meta/sub-statements :meta/authors]))


;; Discussion
(s/def :discussion/title string?)
(s/def :discussion/description string?)
(s/def :discussion/share-hash ::non-blank-string)
(s/def :discussion/edit-hash ::non-blank-string)
(s/def :discussion/share-link ::non-blank-string)
(s/def :discussion/admin-link ::non-blank-string)
(s/def :discussion/author ::user-or-reference)
(s/def :discussion/header-image-url string?)
(s/def :discussion/created-at inst?)
(s/def :discussion/hub-origin (s/or :reference :db/id
                                    :hub ::hub))
(s/def :discussion/admins (s/coll-of (s/or :registered-user ::registered-user
                                           :reference ::entity-reference)))
(s/def :discussion/states
  (s/coll-of #{:discussion.state/open :discussion.state/closed
               :discussion.state/private :discussion.state/deleted
               :discussion.state/public :discussion.state/read-only
               :discussion.state/disable-pro-con}
             :distinct true))
(s/def :discussion/starting-statements (s/coll-of ::statement))
(s/def ::discussion (s/keys :req [:discussion/title :discussion/states
                                  :discussion/share-hash :discussion/author]
                            :opt [:discussion/starting-statements :discussion/description
                                  :discussion/header-image-url :discussion/edit-hash
                                  :discussion/admins :discussion/hub-origin
                                  :discussion/created-at :discussion/share-link :discussion/admin-link]))

(s/def :discussion/meta-info
  (s/keys :req-un [:meta/all-statements :meta/authors]))
(s/def ::discussion-dto (s/keys :req [:discussion/title :discussion/states :discussion/share-hash :discussion/author
                                      :discussion/share-link :discussion/admin-link :discussion/created-at :db/id
                                      :discussion/edit-hash]
                                :opt-un [:discussion/meta-info]))

(s/def :hub/name ::non-blank-string)
(s/def :hub/keycloak-name ::non-blank-string)
(s/def :hub/schnaqs (s/coll-of ::discussion))
(s/def :hub/created-at inst?)
(s/def ::hub (s/keys :req [:hub/name :hub/keycloak-name]
                     :opt [:hub/schnaqs :hub/created-at]))

;; Statement
(s/def :statement/type #{:statement.type/attack :statement.type/support :statement.type/neutral})
(s/def :statement/parent (s/or :id :db/id :statement ::statement))
(s/def :statement/content ::non-blank-string)
(s/def :statement/version number?)
(s/def :statement/author ::any-user)
(s/def :statement/upvotes (s/coll-of ::user-or-reference))
(s/def :statement/downvotes (s/coll-of ::user-or-reference))
(s/def :statement/creation-secret ::non-blank-string)
(s/def :statement/created-at inst?)
(s/def :statement/discussions (s/or :ids (s/coll-of :db/id)
                                    :discussions (s/coll-of ::discussion)))
(s/def ::statement
  (s/keys :req [:statement/content :statement/version :statement/author]
          :opt [:statement/creation-secret :statement/created-at
                :statement/type :statement/parent :statement/discussions]))

(s/def ::statement-dto
  (s/keys :req [:db/id :statement/content :statement/version :statement/created-at
                :statement/author :meta/upvotes :meta/downvotes]
          :opt [:meta/sub-discussion-info]))

(s/def :statement.vote/operation #{:removed :switched :added})

;; Statement via API
(s/def :statement/unqualified-types #{:attack :support :neutral})

;; Common
(s/def :db/id (s/or :transacted number? :temporary any?))
(s/def ::entity-reference (s/or :transacted int? :temporary any?))

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
(s/def ::feedback-dto (s/keys :req-un [:feedback/description :feedback/has-image?]
                              :opt-un [:feedback/contact-name :feedback/contact-mail]))

;; Summary
(s/def :summary/discussion (s/or :id :db/id
                                 :discussion ::discussion))
(s/def :summary/requested-at inst?)
(s/def :summary/created-at inst?)
(s/def :summary/text ::non-blank-string)
(s/def :summary/requester (s/or :id :db/id
                                :registered-user ::registered-user))
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
