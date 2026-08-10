(ns schnaq.interface.sentry
  "Error tracking via Sentry. Without a configured DSN every function in this
  namespace is a no-op, which is the case in local development."
  (:require ["@sentry/browser" :as Sentry]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [re-frame.interceptor :as rf-interceptor]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :as config]
            [taoensso.timbre :as log]))

(def ^:private enabled?
  (not (str/blank? config/sentry-dsn)))

(def ^:private release
  (str "schnaq-frontend@" config/build-hash))

(def ^:private max-context-length
  "Cut off long context values before sending them to Sentry."
  1000)

(defn- truncate
  [value]
  (let [value (str value)]
    (if (> (count value) max-context-length)
      (str (subs value 0 max-context-length) "…")
      value)))

(defn capture-exception
  "Report an exception to Sentry. `context` is an optional map which is attached
  as additional data to the event."
  ([error] (capture-exception error nil))
  ([error context]
   (when enabled?
     (try
       (Sentry/captureException error #js {:extra (clj->js (or context {}))})
       (catch :default e
         (log/warn "Reporting an exception to Sentry failed:" e))))))

(defn capture-message
  "Report a message to Sentry, e.g. a failure which did not throw. `context` is
  an optional map which is attached as additional data to the event."
  ([message] (capture-message message nil "error"))
  ([message context] (capture-message message context "error"))
  ([message context level]
   (when enabled?
     (try
       (Sentry/captureMessage message #js {:level level
                                           :extra (clj->js (or context {}))})
       (catch :default e
         (log/warn "Reporting a message to Sentry failed:" e))))))

(defn set-user!
  "Attach the current user to all following Sentry events."
  [user]
  (when enabled?
    (try
      (Sentry/setUser (clj->js user))
      (catch :default e
        (log/warn "Setting the Sentry user failed:" e)))))

(defn- report-event-error
  "Send errors thrown inside re-frame's interceptor chain to Sentry and hand
  them over to re-frame's default handler, which logs to the console and
  re-throws the original error."
  [original-error re-frame-error]
  (let [{:keys [event-v interceptor direction]} (ex-data re-frame-error)]
    (capture-exception original-error
                       {:event-id (str (first event-v))
                        :event-v (truncate (pr-str event-v))
                        :interceptor (str interceptor)
                        :direction (str direction)}))
  (rf-interceptor/default-error-handler original-error re-frame-error))

(defn init!
  "Start error tracking. Registers a global handler for errors thrown while
  handling re-frame events."
  []
  (if enabled?
    (do
      (Sentry/init #js {:dsn config/sentry-dsn
                        :environment shared-config/environment
                        :release release})
      (rf/reg-event-error-handler report-event-error)
      (log/info (str "[Sentry] Error tracking active for " release)))
    (log/info "[Sentry] No DSN configured, error tracking is disabled")))

;; -----------------------------------------------------------------------------
;; Effects

(rf/reg-fx
 :sentry.error/http-failure
 (fn [failure]
   (let [{:keys [uri status]} failure]
     (capture-message (str "HTTP request failed: " (or status "no status") " " (or uri "unknown uri"))
                      {:http-failure (truncate (pr-str failure))}))))

(rf/reg-fx
 :sentry.user/set
 (fn [user]
   (set-user! user)))
