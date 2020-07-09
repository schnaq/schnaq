(ns meetly.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [meetly.meeting.database :as db]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not (System/getenv "PRODUCTION")
    (spec-test/instrument))
  (db/init))