(ns schnaq.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.main :as db]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not shared-config/production?
    (spec-test/instrument))
  (db/init!))

(comment
  (-main)
  :end)
