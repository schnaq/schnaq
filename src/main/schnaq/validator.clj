(ns schnaq.validator
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.database.discussion :as db]
            [ring.util.http-response :refer [forbidden]]))

(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))

(defn valid-discussion?
  "Check if a schnaq-hash ist valid. Returns false, when the discussion is deleted."
  [share-hash]
  (let [discussion (db/discussion-by-share-hash share-hash)]
    (and discussion
         (not (some #{:discussion.state/deleted} (:discussion/states discussion))))))

(defn valid-writeable-discussion?
  "Check if a schnaq-hash ist valid and writeable. Returns false, when the discussion is deleted or
  should not be written to for any reason."
  [share-hash]
  (let [discussion (db/discussion-by-share-hash share-hash)]
    (and discussion
         (not (some #{:discussion.state/deleted} (:discussion/states discussion)))
         (not (some #{:discussion.state/read-only} (:discussion/states discussion))))))

(defn valid-discussion-and-statement?
  "Checks whether a discussion is valid and also whether the statement belongs to the discussion."
  [statement-id share-hash]
  (and (valid-discussion? share-hash)
       (db/check-valid-statement-id-for-discussion statement-id share-hash)))

(defn valid-writeable-discussion-and-statement?
  "Checks whether a discussion is valid, writeable and also whether the statement belongs to the discussion."
  [statement-id share-hash]
  (and (valid-writeable-discussion? share-hash)
       (db/check-valid-statement-id-for-discussion statement-id share-hash)))

(>defn valid-credentials?
  "Validate if share-hash and edit-hash match"
  [share-hash edit-hash]
  [:discussion/share-hash :discussion/edit-hash :ret boolean?]
  (let [complete-discussion (db/discussion-by-share-hash-private share-hash)]
    (and (= edit-hash (:discussion/edit-hash complete-discussion))
         (not (db/discussion-deleted? share-hash)))))

(defn deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden {:error message})))