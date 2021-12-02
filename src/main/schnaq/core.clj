(ns schnaq.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.main :as db]
            [schnaq.toolbelt :as toolbelt]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not shared-config/production?
    (spec-test/instrument))
  (db/init!)
  (->> (slurp "resources/synonyms/synonyms_german.edn")
       read-string
       (reset! toolbelt/synonyms-german)))

(comment
  (-main)
  :end)
