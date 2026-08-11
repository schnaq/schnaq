(ns schnaq.sentry
  "Optional error tracking via Sentry. Without a configured DSN, e.g. in
  development and in the test-suite, every function in here is a no-op."
  (:require [clojure.string :as str]
            [mount.core :refer [defstate]]
            [schnaq.config :as config]
            [sentry-clj.core :as sentry-clj]
            [taoensso.timbre :as log])
  (:import (java.lang Thread$UncaughtExceptionHandler)))

(defonce ^:private enabled? (atom false))

(defn sentry-configured?
  "True when a Sentry DSN is configured via the `SENTRY_DSN` environment
  variable."
  []
  (not (str/blank? config/sentry-dsn)))

(defn capture-exception!
  "Report `throwable` to Sentry. `context` may contain any of sentry's event
  keys, e.g. `:tags`, `:extra`, `:request` or `:message`. Does nothing when
  Sentry is disabled and never throws."
  ([throwable] (capture-exception! throwable {}))
  ([throwable context]
   (when @enabled?
     (try
       (sentry-clj/send-event
        (merge {:throwable throwable
                :level :error}
               (when-let [message (ex-message throwable)]
                 {:message message})
               context))
       (catch Throwable e
         (log/warn "Could not report exception to Sentry:" (ex-message e)))))
   nil))

(defn- register-uncaught-exception-handler!
  "Log and report exceptions which killed a thread without being handled."
  []
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_this thread throwable]
       (log/error throwable "Uncaught exception in thread" (.getName thread))
       (capture-exception! throwable {:tags {:thread (.getName thread)}})))))

(defn init!
  "Start error tracking if a DSN is configured. Callable directly for entry
  points which do not use mount, e.g. the notification-service. Returns whether
  Sentry has been enabled."
  []
  (if (sentry-configured?)
    (do
      (sentry-clj/init! config/sentry-dsn
                        {:environment config/sentry-environment
                         :release (str "schnaq-backend@" config/build-hash)
                         :in-app-includes ["schnaq"]
                         :enable-uncaught-exception-handler false})
      (reset! enabled? true)
      (register-uncaught-exception-handler!)
      (log/info (format "Sentry enabled for environment %s" config/sentry-environment))
      true)
    (do
      (reset! enabled? false)
      (log/info "Sentry disabled, no SENTRY_DSN configured")
      false)))

(defn shutdown!
  "Close the Sentry client if it has been started."
  []
  (when @enabled?
    (sentry-clj/close!)
    (reset! enabled? false))
  nil)

(defstate sentry-client
  :start (init!)
  :stop (shutdown!))
