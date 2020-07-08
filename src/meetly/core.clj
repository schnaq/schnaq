(ns meetly.core
  (:require [clojure.spec.test.alpha :as spec-test]
            [meetly.meeting.rest-api :as meeting-api]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not (System/getenv "PRODUCTION")
    (spec-test/instrument))
  (meeting-api/api-main))