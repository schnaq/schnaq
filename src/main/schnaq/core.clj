(ns schnaq.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [schnaq.config :as config]
            [schnaq.database.main :as db]))

(def production-mode?
  (= "production" config/env-mode))

;; Used for properly starting the discussion service
(defn -main []
  (when-not production-mode?
    (spec-test/instrument))
  (db/init!))

(comment
  (-main)
  :end)
