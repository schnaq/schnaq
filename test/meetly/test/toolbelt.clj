(ns meetly.test.toolbelt
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [expound.alpha :as expound]
            [meetly.meeting.database :as database]))

(defn create-users-for-dialog-authors
  "Creates a user for every test author. Is only needed in testing, when dialog.core testdata has authors
  for which no users exist."
  []
  (let [author-names (database/all-author-names)]
    (doseq [name author-names]
      (when-not (database/user-by-nickname name)
        (@#'database/transact [{:user/core-author (database/author-id-by-nickname name)}])))))

;; -----------------------------------------------------------------------------
;; Fixtures

(defn init-db-test-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (database/init-and-seed!)
  (create-users-for-dialog-authors)
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