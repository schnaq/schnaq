(ns schnaq.validator
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [forbidden]]
            [schnaq.database.discussion :as db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs :as specs]))

(s/def :ring/response (s/keys :req-un [:http/status :http/headers]))

(defn valid-discussion?
  "Check if a schnaq-hash ist valid. Returns false, when the discussion is deleted."
  [share-hash]
  (try
    (let [discussion (db/discussion-by-share-hash share-hash)]
      (and (s/valid? ::specs/discussion discussion)
           (not (some #{:discussion.state/deleted} (:discussion/states discussion)))))
    (catch Exception _
      false)))

(db/discussion-by-share-hash "123")

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

(>defn user-schnaq-admin?
  "Validate whether the user is a schnaq-admin or not."
  [share-hash keycloak-id]
  [:discussion/share-hash :user.registered/keycloak-id :ret boolean?]
  (let [admins (:discussion/admins
                 (fast-pull [:discussion/share-hash share-hash]
                            [{:discussion/admins [:user.registered/keycloak-id]}]))]
    (not (nil? (some #(= keycloak-id (:user.registered/keycloak-id %)) admins)))))

(defn deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden {:error message})))