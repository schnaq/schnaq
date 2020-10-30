(ns schnaq.meeting.specs
  (:require #?(:clj  [clojure.spec.alpha :as s]
               :cljs [cljs.spec.alpha :as s])
            [clojure.string :as string]))

;; Frontend only
#?(:cljs (s/def :re-frame/component vector?))

;; Discussion
(s/def :discussion/title string?)
(s/def :discussion/description string?)
(s/def :discussion/states
  (s/coll-of #{:discussion.state/open :discussion.state/closed
               :discussion.state/private :discussion.state/deleted}
             :distinct true))
(s/def :discussion/starting-arguments (s/coll-of ::argument))
(s/def :discussion/starting-statements (s/coll-of ::statement))
(s/def ::discussion (s/keys :req [:discussion/title :discussion/description
                                  :discussion/states]
                            :opt [:discussion/starting-arguments :discussion/starting-statements]))

;; Author
(s/def :author/nickname string?)
(s/def ::author (s/keys :req [:author/nickname]))

;; Statement
(s/def :statement/content string?)
(s/def :statement/version number?)
(s/def :statement/author ::author)
(s/def ::statement
  (s/keys :req [:statement/content :statement/version :statement/author]))

;; Argument
(s/def :argument/type
  #{:argument.type/attack :argument.type/support :argument.type/undercut})
(s/def :argument/version number?)
(s/def :argument/author ::author)
(s/def :argument/conclusion (s/or :statement ::statement
                                  :argument ::argument))
(s/def :argument/premises (s/coll-of ::statement))
(s/def :argument/discussions (s/coll-of ::discussion))
(s/def ::argument
  (s/keys :req [:argument/author :argument/premises :argument/conclusion
                :argument/type :argument/version]
          :opt [:argument/discussions]))

;; Common
(s/def :db/id (s/or :transacted number? :temporary any?))
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
                             :author ::author))
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
(s/def :agenda/rank pos-int?)
(s/def ::agenda (s/keys :req [:agenda/title :agenda/meeting :agenda/discussion]
                        :opt [:agenda/description :agenda/rank]))
(s/def ::agenda-essentials-only (s/keys :req [:agenda/title]
                                        :opt [:agenda/description :agenda/rank]))

(s/def :author/nickname ::non-blank-string)

;; Feedback
(s/def :feedback/contact-name string?)
(s/def :feedback/contact-mail string?)
(s/def :feedback/description ::non-blank-string)
(s/def :feedback/has-image? boolean?)
(s/def ::feedback (s/keys :req [:feedback/description :feedback/has-image?]
                          :opt [:feedback/contact-name :feedback/contact-mail]))
