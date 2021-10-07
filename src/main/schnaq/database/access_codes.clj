(ns schnaq.database.access-codes
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.main :refer [transact fast-pull clean-and-add-to-db! query]]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt])
  (:import (java.util Date)))

(>defn- generate-code
  "Generates an access code of a specific length defined in the config."
  []
  [:ret :discussion.access/code]
  (rand-int (Math/pow 10 shared-config/access-code-length)))

(>defn valid?
  "Check if the access-code is correctly configured and not expired."
  [{:discussion.access/keys [created-at expires-at] :as access-code}]
  [:discussion/access :ret boolean?]
  (and (s/valid? :discussion/access access-code)
       (nat-int? (- (.getTime expires-at) (.getTime created-at)))))

(>defn- code-available?
  "Validate, if the provided `code` is not in use. Invalidated / expired codes
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
  "Looks up a discussion and revokes the existing access-code for this
  discussion."
  [discussion-id]
  [:db/id :ret (s/coll-of vector?)]
  (transact
    (for [access-code-ref (query '[:find [?access-code ...]
                                   :in $ ?discussion-id
                                   :where [?access-code :discussion.access/discussion ?discussion-id]]
                                 discussion-id)]
      [:db/retractEntity access-code-ref])))

(defn remove-invalid-and-pull-up-access-codes
  "Remove invalid / expired discussion access codes. Also unpacks the
  access-codes from their collection, because there is always only one valid
  access code.
  This function is obsolete when we implement a scheduler, which periodically
  checks the validity of the access tokens."
  [data]
  (walk/postwalk
    (fn [discussion]
      (if (s/valid? ::specs/discussion discussion)
        (if-let [access-codes (:discussion/access discussion)]
          (let [access-code (first access-codes)]
            (if (valid? access-code)
              (assoc discussion :discussion/access access-code)
              (dissoc discussion :discussion/access)))
          discussion)
        discussion))
    data))


;; -----------------------------------------------------------------------------

(>defn add-access-code-to-discussion!
  "Generate an access code for a discussion. Optionally takes a number of days
  to calculate a custom expiration date of the access-code."
  ([discussion-id]
   [:db/id :ret :discussion/access]
   (add-access-code-to-discussion! discussion-id shared-config/access-code-default-expiration))
  ([discussion-id days-valid]
   [:db/id nat-int? :ret :discussion/access]
   (let [_ (revoke-existing-access-codes discussion-id)
         access-code-ref (clean-and-add-to-db!
                           {:discussion.access/code (find-available-code)
                            :discussion.access/discussion discussion-id
                            :discussion.access/created-at (Date.)
                            :discussion.access/expires-at (toolbelt/now-plus-days-instant days-valid)}
                           :discussion/access)]
     (fast-pull access-code-ref patterns/access-code-with-discussion))))

(>defn discussion-by-access-code
  "Query a discussion by its access code."
  [code]
  [:discussion.access/code :ret (? :discussion/access)]
  (let [access-code (-> (fast-pull [:discussion.access/code code] patterns/access-code-with-discussion)
                        toolbelt/pull-key-up
                        remove-invalid-and-pull-up-access-codes)]
    (when (valid? access-code) access-code)))
