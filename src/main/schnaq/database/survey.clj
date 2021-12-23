(ns schnaq.database.survey
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn- ?]]
            [schnaq.database.main :refer [fast-pull] :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as tools])
  (:import (java.util UUID)))

(>defn- new-survey
  "Create and return a survey entity. Options must be passed as a collection of strings."
  [title survey-type options discussion-id]
  [:survey/title :survey/type (s/coll-of ::specs/non-blank-string) :db/id :ret (? ::specs/survey)]
  (when (< 0 (count options))
    (let [tx @(db/transact [{:db/id "newly-created-survey"
                             :survey/title title
                             :survey/type survey-type
                             :survey/discussion discussion-id
                             :survey/options (mapv (fn [val] {:db/id (.toString (UUID/randomUUID))
                                                              :option/value val}) options)}])
          ;; TODO make this its own function pattern
          new-survey-id (get-in tx [:tempids "newly-created-survey"])
          db-after (:db-after tx)]
      (tools/pull-key-up
       (fast-pull new-survey-id patterns/survey db-after)))))
