(ns meetly.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [meetly.meeting.database :as db]
            [meetly.config :as config]
            [dialog.discussion.database :as dialog]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not (= "production" config/env-mode)
    (spec-test/instrument))
  (db/init!)
  (dialog/init! {:datomic config/datomic
                 :name config/db-name}))

(comment
  (-main)
  (db/delete-database!)
  (dialog/load-testdata!)
  :end)