(ns schnaq.database.surveys
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log]))

(>defn participate-using-schnaq-for-survey
  "Add a new participation to a survey."
  [keycloak-id topics]
  [(? :user.registered/keycloak-id) :surveys.using-schnaq-for/topics => (? map?)]
  (if keycloak-id
    @(db/transact [{:surveys.using-schnaq-for/user [:user.registered/keycloak-id keycloak-id]
                    :surveys.using-schnaq-for/topics topics}])
    (log/error "Could not save survey, keycloak-id was empty.")))

(>defn using-schnaq-for-results
  "Return all results of the survey."
  []
  [=> (s/coll-of :surveys/using-schnaq-for)]
  (->
   (db/query
    '[:find [(pull ?surveys pattern) ...]
      :in $ pattern
      :where [?surveys :surveys.using-schnaq-for/user]]
    patterns/survey-using-schnaq-for)
   toolbelt/pull-key-up))

(def keycloak-id "adb1296a-6398-4384-a946-d8075293ba27")
(def topics [:surveys.using-schnaq-for.topics/coachings :surveys.using-schnaq-for.topics/meetings])

(comment

  (using-schnaq-for-results)

  (db/transact [{:surveys.using-schnaq-for/user [:user.registered/keycloak-id keycloak-id]
                 :surveys.using-schnaq-for/topics topics}])

  nil)
