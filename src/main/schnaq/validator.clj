(ns schnaq.validator
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.meeting.database :as db]
            [ring.util.http-response :refer [forbidden]]
            [schnaq.config :as config]))

(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))

(>defn valid-password?
  "Check if the password is a valid."
  [password]
  [string? :ret boolean?]
  (= config/admin-password password))

(defn valid-hash?
  "Check if a schnaq-hash ist valid."
  [share-hash]
  (not (nil? (db/meeting-by-hash share-hash))))

(>defn valid-credentials?
  "Validate if share-hash and edit-hash match"
  [share-hash edit-hash]
  [string? string? :ret boolean?]
  (let [authenticate-meeting (db/meeting-by-hash-private share-hash)]
    (= edit-hash (:meeting/edit-hash authenticate-meeting))))

(defn deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden {:error message})))