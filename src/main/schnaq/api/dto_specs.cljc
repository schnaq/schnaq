(ns schnaq.api.dto-specs
  "Objects via API are different / reduced compared to our database specs.
  Therefore, we need to specify them here. Avoids name-clashes with this new
  namespace."
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])))

(s/def ::registered-user
  (s/keys :req [:user.registered/keycloak-id :user.registered/display-name]
          :opt [:user.registered/profile-picture]))

;; Statements
(s/def ::statement
  (s/keys :req [:db/id :statement/content :statement/version :statement/created-at
                :statement/author :meta/upvotes :meta/downvotes]
          :opt [:meta/sub-discussion-info]))

(s/def :statement/unqualified-types #{:attack :support :neutral})

;; Discussions
(s/def :discussion/meta-info
  (s/keys :req-un [:meta/all-statements :meta/authors]))
(s/def ::discussion
  (s/keys :req [:discussion/title :discussion/share-hash :discussion/author
                :discussion/created-at]
          :opt [:discussion/share-link :discussion/admin-link :discussion/edit-hash
                :discussion/states :db/id]
          :opt-un [:discussion/meta-info]))

;; Feedbacks
(s/def ::feedback
  (s/keys :req-un [:feedback/description :feedback/has-image?]
          :opt-un [:feedback/contact-name :feedback/contact-mail]))


;; Summaries
(s/def ::summary
  (s/keys :req [:db/id]))