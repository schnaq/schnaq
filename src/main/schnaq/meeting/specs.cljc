(ns schnaq.meeting.specs
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.string :as string]))

;; Frontend only
#?(:cljs (s/def :re-frame/component vector?))

;; Common
(s/def :db/id int?)
(s/def ::entity-reference (s/or :transacted int? :temporary any?))
(s/def ::non-blank-string (s/and string? (complement string/blank?)))

;; Meeting
(s/def :meeting/title ::non-blank-string)
(s/def :meeting/description ::non-blank-string)
(s/def :meeting/share-hash ::non-blank-string)
(s/def :meeting/edit-hash ::non-blank-string)
(s/def :meeting/start-date inst?)
(s/def :meeting/end-date inst?)
(s/def :meeting/author (s/or :reference ::entity-reference
                             :author :dialog.discussion.models/author))
(s/def ::meeting (s/keys :req [:meeting/title :meeting/author
                               :meeting/share-hash
                               :meeting/start-date :meeting/end-date]
                         :opt [:meeting/description :meeting/edit-hash]))

(s/def ::meeting-without-hashes (s/keys :req [:meeting/title :meeting/author]
                                        :opt [:meeting/description]))

;; Agenda
(s/def :agenda/title ::non-blank-string)
(s/def :agenda/description ::non-blank-string)
(s/def :agenda/meeting ::entity-reference)
(s/def :agenda/discussion ::entity-reference)
(s/def ::agenda (s/keys :req [:agenda/title :agenda/meeting :agenda/discussion]
                        :opt [:agenda/description]))
(s/def ::agenda-without-discussion (s/keys :req [:agenda/title :agenda/meeting]
                                           :opt [:agenda/description]))

(s/def :author/nickname ::non-blank-string)

;; Feedback
(s/def :feedback/contact-name string?)
(s/def :feedback/contact-mail string?)
(s/def :feedback/description ::non-blank-string)
(s/def :feedback/has-image? boolean?)
(s/def ::feedback (s/keys :req [:feedback/description :feedback/has-image?]
                          :opt [:feedback/contact-name :feedback/contact-mail]))