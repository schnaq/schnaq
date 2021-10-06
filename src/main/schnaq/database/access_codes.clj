(ns schnaq.database.access-codes
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
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
  "Validate, if the provided `code` is not in use."
  [code]
  [:discussion.access/code :ret boolean?]
  (let [access-code (main-db/fast-pull [:discussion.access/code code])]
    (or (nil? (:db/id access-code))
        (not (valid? access-code)))))

(>defn- find-available-code
  "Returns for an available code, which can be used to access a discussion."
  []
  [:ret :discussion.access/code]
  (loop [code (generate-code)]
    (if (code-available? code)
      code
      (recur (generate-code)))))


;; -----------------------------------------------------------------------------

(>defn add-access-code-to-discussion
  "Generate an access code for a discussion."
  [share-hash days-valid]
  [:discussion/share-hash nat-int? :ret ::specs/access-code]
  (let [access-code-ref (main-db/clean-and-add-to-db!
                          {:discussion.access/code (find-available-code)
                           :discussion.access/discussion [:discussion/share-hash share-hash]
                           :discussion.access/created-at (Date.)
                           :discussion.access/expires-at (toolbelt/now-plus-days-instant days-valid)}
                          ::specs/access-code)]
    (main-db/fast-pull access-code-ref access-code-pattern)))

(>defn discussion-by-access-code
  "Query a discussion by its access code."
  [code]
  [:discussion.access/code :ret ::specs/access-code]
  (toolbelt/pull-key-up
    (main-db/fast-pull [:discussion.access/code code] access-code-pattern)))


(comment
  (code-available? 42)



  (def sample {:db/id 17592186045451,
               :discussion.access/code 42,
               :discussion.access/discussion [:discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0"]
               :discussion.access/created-at #inst"2021-10-06T10:56:36.257-00:00",
               :discussion.access/expires-at #inst"2021-10-07T12:56:36.257-00:00"})



  (main-db/transact [[:db/retract [:discussion.access/code 42] :discussion.access/discussion]])
  (main-db/fast-pull [:discussion.access/code 42111])


  (main-db/clean-and-add-to-db!
    {:discussion.access/code 42
     :discussion.access/discussion [:discussion/share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0"]
     :discussion.access/created-at (Date.)
     :discussion.access/expires-at (toolbelt/now-plus-days-instant 1)}
    ::specs/access-code)
  34


  (discussion-db/discussion-by-share-hash "1ea965de-bb39-4ae9-85b2-f3b3bad12af0")

  (add-access-code-to-discussion "1ea965de-bb39-4ae9-85b2-f3b3bad12af0" 1)

  nil)
