(ns schnaq.meeting.models)

(def datomic-schema
  [{:db/ident :meeting/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The Title for the Meeting - does not have to be unique"}
   {:db/ident :meeting/start-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time the meetings officially starts at."}
   {:db/ident :meeting/end-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "The time after which participation is not possible anymore."}
   {:db/ident :meeting/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A description of the meetings purpose."}
   {:db/ident :meeting/share-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A hash that grants participation access to the discussion"}
   {:db/ident :meeting/edit-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "A hash that grants edit access to the discussion"}
   {:db/ident :meeting/author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The author of a meeting."}

   ;; Suggesting changes to a meeting
   {:db/ident :meeting.suggestion/meeting
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Suggestion for a meeting"}
   {:db/ident :meeting.suggestion/ideator
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user making the suggestion"}
   {:db/ident :meeting.suggestion/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Suggestion to change title of a meeting"}
   {:db/ident :meeting.suggestion/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Suggestion to change description of a meeting"}

   ;; Suggesting agenda update
   {:db/ident :agenda.suggestion/agenda
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Suggestion to change agenda"}
   {:db/ident :agenda.suggestion/ideator
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user making the suggestion"}
   {:db/ident :agenda.suggestion/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "New title for agenda"}
   {:db/ident :agenda.suggestion/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "New title for agenda"}
   {:db/ident :agenda.suggestion/type
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Indicate the update on the agenda"}
   {:db/ident :agenda.suggestion/meeting
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "Referring (new) agenda to an existing meeting"}
   ;; Meeting Feedback (NOT user Feedback for schnaq)
   {:db/ident :meeting.feedback/ideator
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The user giving the feedback"}
   {:db/ident :meeting.feedback/content
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The content of the feedback"}
   {:db/ident :meeting.feedback/meeting
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "For which meeting is the feedback?"}

   ;; Valid suggestion types on an agenda
   {:db/ident :agenda.suggestion.type/update}
   {:db/ident :agenda.suggestion.type/new}
   {:db/ident :agenda.suggestion.type/delete}

   ;; Agenda-Point
   {:db/ident :agenda/title
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The short title of the Agenda-Point"}
   {:db/ident :agenda/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A description of the point"}
   {:db/ident :agenda/meeting
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc "The meeting the agenda belongs to"}
   {:db/ident :agenda/discussion
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "An id belonging to the (foreign) discussion represented by this agenda"}
   ;; User
   {:db/ident :user/core-author
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db/doc "The author of dialog.core that corresponds to this user."}
   {:db/ident :user/upvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All upvotes the user gave."}
   {:db/ident :user/downvotes
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/doc "All downvotes the user gave."}
   ;; Feedback
   {:db/ident :feedback/contact-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Name of the person who gave feedback"}
   {:db/ident :feedback/contact-mail
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "How to contact the person who gave feedback"}
   {:db/ident :feedback/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "The feedback description."}
   {:db/ident :feedback/has-image?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Indicate wether a user provided an image."}])
