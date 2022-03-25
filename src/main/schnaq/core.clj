(ns schnaq.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [mount.core :refer [defstate]]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.main :as db]
            [schnaq.toolbelt :as toolbelt]))

(defstate database
  :start (db/init!))

(defstate synonyms
  :start (->> (slurp "https://s3.schnaq.com/synonyms/synonyms_german.edn")
              read-string
              (reset! toolbelt/synonyms-german)))

(defstate spec-instrumentation
  :start (when-not shared-config/production?
           (spec-test/instrument))

  :stop (when-not shared-config/production?
          (spec-test/unstrument)))
