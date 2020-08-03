(ns meetly.test.toolbelt
  (:require [meetly.meeting.database :as database]
            [dialog.discussion.database :as dialog]))


(defn init-db-test-fixture
  "Fixture to initialize, test, and afterwards delete the database."
  [f]
  (database/init!)
  (dialog/init-and-seed!)
  (f))