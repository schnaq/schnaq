(ns dialog.discussion.core
  (:require [clojure.spec.test.alpha :as spec-test]))

;; Used for properly starting the discussion service
(defn -main []
  (when-not (System/getenv "PRODUCTION")
    (spec-test/instrument)))