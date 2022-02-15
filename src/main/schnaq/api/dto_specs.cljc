(ns schnaq.api.dto-specs
  "Objects via API are different / reduced compared to our database specs.
  Therefore, we need to specify them here. Avoids name-clashes with this new
  namespace."
  (:require [schnaq.database.specs :as specs]
            [spec-tools.core :as st]
            #?(:clj [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])))

;; Users
(s/def ::registered-user
  (s/keys :req [:user.registered/keycloak-id :user.registered/display-name]
          :opt [:user.registered/profile-picture]))

(s/def ::any-user (s/or :dto ::registered-user
                        :user ::specs/user
                        :registered-user ::specs/registered-user))

(s/def ::maybe-nickname
  (s/or :nil nil?
        :nickname :user/nickname))

;; Statements
(s/def ::statement
  (s/keys :req [:db/id :statement/content :statement/version :statement/created-at
                :statement/author]
          :opt [:meta/sub-statement-count :statement/upvotes :statement/downvotes :statement/labels
                :statement/type]))

(def statement-type (st/spec :statement/type {:type :keyword}))
(s/def :statement/unqualified-types #{:attack :support :neutral})

(def poll-type (st/spec :poll/type {:type :keyword}))
(s/def ::poll
  (s/keys :req [:db/id :poll/title :poll/type :poll/options]
          :opt [:poll/discussion]))

;; Discussions
(s/def :discussion/meta-info
  (s/keys :req-un [:meta/all-statements :meta/authors]))
(s/def ::discussion
  (s/keys :req [:discussion/title :discussion/share-hash :discussion/author
                :discussion/created-at]
          :opt [:discussion/share-link :discussion/admin-link :discussion/edit-hash
                :discussion/states :db/id :discussion/creation-secret :discussion/mode]
          :opt-un [:discussion/meta-info]))

(s/def ::discussion-title :discussion/title)
(s/def ::hub-exclusive? boolean?)
(s/def ::hub :hub/keycloak-name)
(s/def ::discussion-add-body
  (s/keys :req-un [::discussion-title]
          :opt-un [:user/nickname ::hub-exclusive? ::hub]))

;; Feedbacks
(s/def ::feedback
  (s/keys :req [:feedback/description :feedback/has-image?]
          :opt [:feedback/contact-name :feedback/contact-mail]))

;; Summaries
(s/def ::summary
  (s/keys :req [:summary/requested-at]
          :opt [:summary/discussion :summary/text :summary/created-at :summary/requester]))
