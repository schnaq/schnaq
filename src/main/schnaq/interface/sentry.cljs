(ns schnaq.interface.sentry
  "Error tracking via Sentry. Without a configured DSN every function in this
  namespace is a no-op, which is the case in local development."
  (:require ["@sentry/browser" :as Sentry]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [re-frame.interceptor :as rf-interceptor]
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
                       ;; Only the event id, never its arguments: they routinely
                       ;; carry share hashes and whatever the user typed.
                       {:event-id (str (first event-v))
                        :event-argument-count (count (rest event-v))
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
                        :environment config/sentry-environment
                        :release release})
      (rf/reg-event-error-handler report-event-error)
      (log/info (str "[Sentry] Error tracking active for " release)))
    (log/info "[Sentry] No DSN configured, error tracking is disabled")))

;; -----------------------------------------------------------------------------
;; Effects

(defn request-path
  "The path of `uri` without its query string. `day8.re-frame.http-fx` reports
  the full URL, and GET parameters carry share hashes and access codes. Those
  are the credentials to a discussion and must never reach Sentry."
  [uri]
  (when-not (str/blank? uri)
    (first (str/split uri #"\?"))))

(defn report-http-failure?
  "Decide whether a failed request is worth an event. Client errors are answers,
  not defects: a missing schnaq, an expired token or a forbidden action all
  arrive as 4xx and say nothing about the health of the application. Aborted,
  timed out and failed requests describe the client's network and would drown
  everything else on mobile connections."
  [{:keys [status failure]}]
  (case failure
    :error (>= (or status 0) 500)
    (:parse :exception) true
    false))

(rf/reg-fx
 :sentry.error/http-failure
 (fn [{:keys [uri status status-text failure] :as http-failure}]
   (when (report-http-failure? http-failure)
     (let [path (request-path uri)]
       ;; The response body stays out on purpose. The backend reports its own
       ;; server errors with a full stacktrace, so the frontend only has to
       ;; contribute the signal that a request failed.
       (capture-message (str "HTTP " (or status "?") " " (or path "unknown path"))
                        {:status status
                         :status-text (truncate status-text)
                         :failure (str failure)
                         :path path})))))

(rf/reg-fx
 :sentry.user/set
 (fn [user]
   (set-user! user)))
