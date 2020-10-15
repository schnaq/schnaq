(ns schnaq.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [dialog.discussion.database :as dialog]
            [schnaq.config :as config]
            [schnaq.meeting.database :as db]))

(def production-mode?
  (= "production" config/env-mode))

;; Used for properly starting the discussion service
(defn -main []
  (when-not production-mode?
    (spec-test/instrument))
  (db/init!)
  (dialog/init! {:datomic config/datomic
                 :name config/db-name}))

(comment
  (-main)
  :end)