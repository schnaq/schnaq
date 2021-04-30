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
(s/def ::registered-user (s/keys :req [:user.registered/keycloak-id :user.registered/email
                                       :user.registered/display-name]
                                 :opt [:user.registered/last-name :user.registered/first-name
                                       :user.registered/groups :user.registered/profile-picture]))

;; Could be anonymous or registered
(s/def ::any-user (s/or :user ::user :registered-user ::registered-user))
;; Any user or reference
(s/def ::user-or-reference (s/or :reference ::entity-reference
                                 :user ::user
                                 :registered-user ::registered-user))

;; Discussion
(s/def :discussion/title string?)
(s/def :discussion/description string?)
(s/def :discussion/share-hash ::non-blank-string)
(s/def :discussion/edit-hash ::non-blank-string)
(s/def :discussion/author ::user-or-reference)
(s/def :discussion/header-image-url string?)
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
                                  :db/txInstant :discussion/admins :discussion/hub-origin]))

(s/def :hub/name ::non-blank-string)
(s/def :hub/keycloak-name ::non-blank-string)
(s/def :hub/schnaqs (s/coll-of ::discussion))
(s/def ::hub (s/keys :req [:hub/name :hub/keycloak-name]
                     :opt [:hub/schnaqs :db/txInstant]))

;; Statement
(s/def :statement/content ::non-blank-string)
(s/def :statement/version number?)
(s/def :statement/author ::any-user)
(s/def :statement/upvotes (s/coll-of ::user-or-reference))
(s/def :statement/downvotes (s/coll-of ::user-or-reference))
(s/def :statement/creation-secret ::non-blank-string)
(s/def ::statement
  (s/keys :req [:statement/content :statement/version :statement/author]
          :opt [:statement/creation-secret]))

;; Argument
(s/def :argument/type
  #{:argument.type/attack :argument.type/support :argument.type/undercut :argument.type/neutral})
(s/def :argument/version number?)
(s/def :argument/author (s/or :user ::user :registered-user ::registered-user))
(s/def :argument/conclusion (s/or :statement ::statement
                                  :argument ::argument))
(s/def :argument/premises (s/coll-of ::statement))
(s/def :argument/discussions (s/coll-of ::discussion))
(s/def ::argument
  (s/keys :req [:argument/author :argument/premises :argument/conclusion
                :argument/type :argument/version]
          :opt [:argument/discussions
                :db/txInstant]))

;; Common
(s/def :db/id (s/or :transacted number? :temporary any?))
(s/def ::entity-reference (s/or :transacted int? :temporary any?))

;; Feedback
(s/def :feedback/contact-name string?)
(s/def :feedback/contact-mail string?)
(s/def :feedback/description ::non-blank-string)
(s/def :feedback/has-image? boolean?)
(s/def ::feedback (s/keys :req [:feedback/description :feedback/has-image?]
                          :opt [:feedback/contact-name :feedback/contact-mail
                                :db/txInstant]))
