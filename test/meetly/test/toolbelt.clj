(ns meetly.test.toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [datomic.dev-local :as dev-local]
            [expound.alpha :as expound]
            [meetly.meeting.database :as database]))


;; -----------------------------------------------------------------------------
;; Fixtures

(defn init-db-test-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (database/init-and-seed!)
  (f))

(defn init-test-delete-db-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (init-db-test-fixture f)
  (database/delete-database-from-config!))

;; -----------------------------------------------------------------------------
;; Generative Test Helpers

(defn- passed-all-tests?
  "`check` returns a list of tests. Get all results of these tests and check
  that they are all true."
  [results]
  (every? true?
          (map #(get-in % [:clojure.spec.test.check/ret :pass?]) results)))

(defn check?
  "Given a fully qualified function name, apply generative tests and pretty
  print the results, if there are any errors."
  [fn]
  (binding [s/*explain-out* expound/printer]
    (let [test-results (stest/check fn)]
      (if (passed-all-tests? test-results)
        true
        (do (expound/explain-results test-results) false)))))