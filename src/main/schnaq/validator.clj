(ns schnaq.validator
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.meeting.database :as db]
            [ring.util.http-response :refer [forbidden]]
            [schnaq.config :as config]))

(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))

(>defn valid-password?
  "Check if the password is a valid."
  [password]
  [string? :ret boolean?]
  (= config/admin-password password))

(defn valid-discussion?
  "Check if a schnaq-hash ist valid. Returns false, when the discussion is deleted."
  [share-hash]
  (and (discussion-db/discussion-by-share-hash share-hash)
       (not (discussion-db/discussion-deleted? share-hash))))

(defn valid-discussion-and-statement?
  "Checks whether a discussion is valid and also whether the statement belongs to the discussion."
  [statement-id share-hash]
  (and (valid-discussion? share-hash)
       (db/check-valid-statement-id-and-meeting statement-id share-hash)))

(>defn valid-credentials?
  "Validate if share-hash and edit-hash match"
  [share-hash edit-hash]
  [:discussion/share-hash :discussion/edit-hash :ret boolean?]
  (let [complete-discussion (discussion-db/discussion-by-share-hash-private share-hash)]
    (and (= edit-hash (:discussion/edit-hash complete-discussion))
         (not (discussion-db/discussion-deleted? share-hash)))))

(defn deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden {:error message})))