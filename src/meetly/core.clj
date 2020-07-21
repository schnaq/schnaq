(ns meetly.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [meetly.meeting.database :as db]
            [meetly.config :as config]
            [dialog.discussion.database :as dialog]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not (System/getenv "PRODUCTION")
    (spec-test/instrument))
  (db/init)
  (dialog/init! {:datomic config/datomic
                 :name config/db-name}))

(comment
  (-main)
  (dialog/load-testdata!)
  :end)