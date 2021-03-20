(ns schnaq.meeting.specs
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.string :as string]))

;; Frontend only
#?(:cljs (s/def :re-frame/component vector?))

;; Transaction
(s/def :db/txInstant inst?)

;; Discussion
(s/def ::non-blank-string (s/and string? (complement string/blank?)))

(s/def :discussion/title string?)
(s/def :discussion/description string?)
(s/def :discussion/share-hash ::non-blank-string)
(s/def :discussion/edit-hash ::non-blank-string)
(s/def :discussion/author (s/or :reference ::entity-reference
                                :user ::user))
(s/def :discussion/header-image-url string?)
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
                                  :db/txInstant]))

(s/def :hub/name string?)
(s/def :hub/schnaqs (s/coll-of ::discussion))
(s/def ::hub (s/keys :req [:hub/name]
                     :opt [:hub/schnaqs]))

;; Author
(s/def :user/nickname string?)
(s/def ::user (s/keys :req [:user/nickname]))

;; Statement
(s/def :statement/content string?)
(s/def :statement/version number?)
(s/def :statement/author ::user)
(s/def ::statement
  (s/keys :req [:statement/content :statement/version :statement/author]))

;; Argument
(s/def :argument/type
  #{:argument.type/attack :argument.type/support :argument.type/undercut})
(s/def :argument/version number?)
(s/def :argument/author ::user)
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
