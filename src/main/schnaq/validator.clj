(ns schnaq.validator
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn ? =>]]
            [ring.util.http-response :refer [forbidden]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]))

(defn valid-discussion?
  "Check if a schnaq-hash is valid. Returns false, when the discussion is deleted."
  [share-hash]
  (try
    (let [discussion (db/discussion-by-share-hash share-hash)]
      (and (s/valid? ::specs/discussion discussion)
           (not (some #{:discussion.state/deleted} (:discussion/states discussion)))))
    (catch Exception _
      false)))

(defn valid-open-discussion?
  "Check if a schnaq-hash ist valid and writeable. Returns false, when the discussion is deleted or
  should not be written to for any reason."
  [share-hash]
  (let [discussion (db/discussion-by-share-hash share-hash)]
    (and discussion
         (not (some #{:discussion.state/deleted} (:discussion/states discussion)))
         (not (some #{:discussion.state/read-only} (:discussion/states discussion))))))

(defn posts-allowed?
  "Check whether it is permissible to write posts. (Not necessarily the same as read only,
  since interactions can be allowed still)"
  [share-hash]
  (let [discussion (db/discussion-by-share-hash share-hash)]
    (and discussion (not (some #{:discussion.state/disable-posts} (:discussion/states discussion))))))

(defn valid-discussion-and-statement?
  "Checks whether a discussion is valid and also whether the statement belongs to the discussion."
  [statement-id share-hash]
  (and (valid-discussion? share-hash)
       (db/check-valid-statement-id-for-discussion statement-id share-hash)))

(>defn user-moderator?
  "Validate, whether a user is a moderator. Authors are implicitly always the moderators."
  [share-hash user-id]
  [(? :discussion/share-hash) (? :db/id) => (? boolean?)]
  (when (and share-hash user-id)
    (let [discussion (fast-pull [:discussion/share-hash share-hash] patterns/discussion)]
      (or (= user-id (:db/id (:discussion/author discussion)))
          (contains? (set (:discussion/moderators discussion)) user-id)))))

(defn deny-access
  "Return a 403 Forbidden to unauthorized access."
  ([]
   (deny-access "You are not allowed to access this resource."))
  ([message]
   (forbidden (at/build-error-body :auth/access-denied message))))
