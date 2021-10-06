(ns schnaq.database.access-codes
  (:require [ghostwheel.core :refer [>defn >defn-]]
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

(>defn- generate-access-code
  "Generates an access code of a specific length defined in the config."
  []
  [:ret :discussion.access/code]
  (rand-int (Math/pow 10 shared-config/access-code-length)))

(>defn add-access-code-to-discussion
  [share-hash days-valid]
  [:discussion/share-hash nat-int? :ret number?]
  (main-db/clean-and-add-to-db!
    {:discussion.access/code (generate-access-code)
     :discussion.access/discussion [:discussion/share-hash share-hash]
     :discussion.access/created-at (Date.)
     :discussion.access/expires-at (toolbelt/now-plus-days-instant days-valid)}
    ::specs/access-code))

(defn discussion-by-access-code
  "Query a discussion by its access code."
  [access-code]
  (toolbelt/pull-key-up
    (main-db/fast-pull [:discussion.access/code access-code] access-code-pattern)))
