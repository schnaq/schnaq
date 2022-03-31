(ns schnaq.api.profiling
  "Tools to profile function calls and API requests.
  Important: When you read from `stats-accumulator`, it resets itself. So you
  can read the information only once."
  (:require [mount.core :as mount :refer [defstate]]
            [schnaq.api.middlewares :refer [extract-parameter-from-request]]
            [schnaq.config.shared :as shared-config]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.tufte :as tufte]))

(declare stats-accumulator)

(defstate stats-accumulator
  :start (when-not shared-config/production?
           (tufte/add-accumulating-handler! {:ns-pattern "*"}))
  :stop (constantly nil))

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

(defn print-profiling-results
  "Print profiling results on the console."
  []
  (when stats-accumulator
    (when-let [stats (not-empty @stats-accumulator)]
      (println (tufte/format-grouped-pstats stats)))))

(defn post-profiling-to-mattermost
  "Post profiling information to mattermost. Channel must be a slug of a real
  channel, e.g. `gitlabs-dirty-secrets`"
  [channel]
  (when stats-accumulator
    (when-let [stats (not-empty @stats-accumulator)]
      (toolbelt/post-in-mattermost!
       (format "```%n%s%n```"
               (tufte/format-grouped-pstats stats))
       channel))))

(comment

  (print-profiling-results)

  (post-profiling-to-mattermost "gitlabs-dirty-secrets")

  nil)
