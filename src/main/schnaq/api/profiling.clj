(ns schnaq.api.profiling
  (:require [mount.core :as mount :refer [defstate]]
            [schnaq.api.middlewares :refer [extract-parameter-from-request]]
            [schnaq.config.shared :as shared-config]
            [schnaq.shared-toolbelt :as shared-tools]
            [taoensso.tufte :as tufte]))

(declare stats-accumulator)

(defstate stats-accumulator
  :start (when-not shared-config/production?
           (tufte/add-accumulating-handler! {:ns-pattern "*"})))

(defn profiling-middleware
  "Add a profiling middleware. Omits all calls to profiler if profiling is not 
  active."
  [handler]
  (fn [request]
    (if stats-accumulator
      (let [api-name (or
                      (->> request :reitit.core/match :data :name)
                      (->> request :reitit.core/match :template shared-tools/slugify (format "unnamed-api-call/%s") keyword))]
        (tufte/profile
         {:id (extract-parameter-from-request request :share-hash)}
         (tufte/p api-name (handler request))))
      (handler request))))
