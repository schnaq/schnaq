(ns schnaq.api.dto-specs
  "Objects via API are different / reduced compared to our database specs.
  Therefore, we need to specify them here. Avoids name-clashes with this new
  namespace."
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])))

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
  (s/keys :req [:discussion/title :discussion/states :discussion/share-hash :discussion/author
                :discussion/created-at :db/id]
          :opt [:discussion/share-link :discussion/admin-link :discussion/edit-hash]
          :opt-un [:discussion/meta-info]))

;; Feedbacks
(s/def ::feedback
  (s/keys :req-un [:feedback/description :feedback/has-image?]
          :opt-un [:feedback/contact-name :feedback/contact-mail]))
