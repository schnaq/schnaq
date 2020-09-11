(ns schnaq.suggestions
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.meeting.database :as db]))

(s/def ::new-meeting-suggestion-input (s/keys :req [:meeting/title :db/id :meeting/share-hash]
                                              :opt [:meeting/description]))

(>defn new-meeting-suggestion
  "Adds a new meeting suggestions, if it has any changes."
  [suggestion user-id]
  [::new-meeting-suggestion-input :db/id :ret :db/id]
  (let [original-meeting (db/meeting-by-hash (:meeting/share-hash suggestion))
        updated-meeting (select-keys suggestion [:db/id :meeting/title :meeting/description])]
    (when (or (not= (:meeting/title original-meeting) (:meeting/title suggestion))
              (not= (:meeting/description original-meeting) (:meeting/description suggestion)))
      (db/suggest-meeting-updates! updated-meeting user-id))))

(>defn- does-agenda-suggestion-change-anything?
  "Check if an agenda suggestion contains changes compared to the database
  entity."
  [suggestion]
  [:db/agenda-suggestion-input :ret boolean?]
  (let [original-agenda (db/agenda (:db/id suggestion))]
    (or (not= (:agenda/title original-agenda) (:agenda/title suggestion))
        (not= (:agenda/description original-agenda) (:agenda/description suggestion)))))

(>defn new-agenda-updates-suggestion
  "Adds agenda-suggestions, if they represent changes."
  [suggestions user-id]
  [:db/agenda-suggestion-input :db/id :ret any?]
  (db/suggest-agenda-updates!
    (filter does-agenda-suggestion-change-anything? suggestions)
    user-id))