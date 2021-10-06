(ns schnaq.database.access-codes
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [transact fast-pull clean-and-add-to-db! query]]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt])
  (:import (java.util Date)))

(def ^:private access-code-pattern
  [:db/id
   :discussion.access/code
   {:discussion.access/discussion discussion-db/discussion-pattern}
   :discussion.access/created-at
   :discussion.access/expires-at])

(>defn- generate-code
  "Generates an access code of a specific length defined in the config."
  []
  [:ret :discussion.access/code]
  (rand-int (Math/pow 10 shared-config/access-code-length)))

(>defn- valid?
  "Check if the access-code is correctly configured and not expired."
  [{:discussion.access/keys [created-at expires-at] :as access-code}]
  [::specs/access-code :ret boolean?]
  (and (s/valid? ::specs/access-code access-code)
       (nat-int? (- (.getTime expires-at) (.getTime created-at)))))

(>defn- code-available?
  "Validate, if the provided `code` is not in use. Invalidated codes
  or free codes can be used."
  [code]
  [:discussion.access/code :ret boolean?]
  (let [access-code (fast-pull [:discussion.access/code code])]
    (or (nil? (:db/id access-code))
        (not (valid? access-code)))))

(>defn- find-available-code
  "Searches for an available code, which can be used to access a discussion."
  []
  [:ret :discussion.access/code]
  (loop [code (generate-code)]
    (if (code-available? code)
      code
      (recur (generate-code)))))

(>defn- revoke-existing-access-codes
  "Looks up all discussions and revokes the existing access-codes for them."
  [share-hash]
  [:discussion/share-hash :ret vector?]
  (transact
    (for [access-code-ref (query '[:find [?access-code ...]
                                   :in $ ?share-hash
                                   :where [?discussion :discussion/share-hash ?share-hash]
                                   [?access-code :discussion.access/discussion ?discussion]]
                                 share-hash)]
      [:db/retract access-code-ref :discussion.access/discussion])))


;; -----------------------------------------------------------------------------

(>defn add-access-code-to-discussion
  "Generate an access code for a discussion."
  [share-hash days-valid]
  [:discussion/share-hash nat-int? :ret ::specs/access-code]
  (let [_ (revoke-existing-access-codes share-hash)
        access-code-ref (clean-and-add-to-db!
                          {:discussion.access/code (find-available-code)
                           :discussion.access/discussion [:discussion/share-hash share-hash]
                           :discussion.access/created-at (Date.)
                           :discussion.access/expires-at (toolbelt/now-plus-days-instant days-valid)}
                          ::specs/access-code)]
    (fast-pull access-code-ref access-code-pattern)))

(>defn discussion-by-access-code
  "Query a discussion by its access code."
  [code]
  [:discussion.access/code :ret (? ::specs/access-code)]
  (let [access-code (toolbelt/pull-key-up
                      (fast-pull [:discussion.access/code code] access-code-pattern))]
    (when (:db/id access-code) access-code)))

(comment


  (discussion-by-access-code 43236077)


  nil)
