(ns schnaq.test.toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [expound.alpha :as expound]
            [schnaq.meeting.database :as database]
            [schnaq.toolbelt :as schnaq-toolbelt]))

;; -----------------------------------------------------------------------------
;; Fixtures
(def ^:private test-config
  {:datomic {:system "test"
             :server-type :dev-local
             :storage-dir (schnaq-toolbelt/create-directory! ".datomic/dev-local/test-data")}
   :name "test-db"})

(defn clean-database-fixture
  "Cleans the database. Should be used with :once to start with a clean sheet."
  [f]
  (database/init! test-config)
  (database/delete-database!)
  (f))

(defn init-db-test-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (database/init-and-seed! test-config)
  (f))

(defn init-test-delete-db-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (init-db-test-fixture f)
  (database/delete-database!))

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