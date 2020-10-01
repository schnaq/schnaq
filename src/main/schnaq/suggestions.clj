(ns schnaq.suggestions
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.meeting.database :as db]
            [clojure.string :as string]))

(>defn new-meeting-suggestion
  "Adds a new meeting suggestions, if it has any changes."
  [suggestion user-id]
  [map? :db/id :ret (? :db/id)]
  (let [original-meeting (db/meeting-by-hash (:meeting/share-hash suggestion))
        updated-meeting (select-keys suggestion [:db/id :meeting/title :meeting/description])]
    (when
      (or (not= (:meeting/title original-meeting) (:meeting/title suggestion))
          ;; If they are truly different the original can not be nil and the new one blank at the same time
          (and (not= (:meeting/description original-meeting) (:meeting/description suggestion))
               (not (and (nil? (:meeting/description original-meeting))
                         (not (string/blank? (:meeting/description updated-meeting)))))))
      (db/suggest-meeting-updates! updated-meeting user-id))))

(>defn- does-agenda-suggestion-change-anything?
  "Check if an agenda suggestion contains changes compared to the database
  entity."
  [suggestion]
  [map? :ret boolean?]
  (let [original-agenda (db/agenda (:db/id suggestion))]
    (or (not= (:agenda/title original-agenda) (:agenda/title suggestion))
        (and (not= (:agenda/description original-agenda) (:agenda/description suggestion))
             (not (and (nil? (:agenda/description original-agenda))
                       (not (string/blank? (:agenda/description suggestion)))))))))

(>defn new-agenda-updates-suggestion
  "Adds agenda-suggestions, if they represent changes."
  [suggestions user-id]
  [(s/coll-of map?) :db/id :ret any?]
  (db/suggest-agenda-updates!
    (filter does-agenda-suggestion-change-anything? suggestions)
    user-id))

(>defn update-agenda
  "Updates an agenda, only if the share-hash matches the :db/id of the agenda. Otherwise just returns
  the agenda."
  [new-agenda share-hash]
  [map? :meeting/share-hash :ret map?]
  (let [meeting-id (:db/id (db/meeting-by-hash share-hash))
        old-agenda (db/agenda (:db/id new-agenda))]
    (if (and (= (:db/id old-agenda) (:db/id new-agenda))
             (= meeting-id (:db/id (:agenda/meeting old-agenda))))
      (db/agenda (db/update-agenda new-agenda))
      new-agenda)))

(>defn update-meeting
  "Updates meeting information and returns the newly updated meeting."
  [new-meeting share-hash]
  [map? :meeting/share-hash :ret map?]
  (let [actual-meeting-id (:db/id (db/meeting-by-hash share-hash))]
    (if (= actual-meeting-id (:db/id new-meeting))
      (do (db/update-meeting new-meeting)
          (db/meeting-by-hash-private share-hash))
      new-meeting)))