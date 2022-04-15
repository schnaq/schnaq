(ns schnaq.toolbelt
  "Utility functions supporting the backend."
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [com.fulcrologic.guardrails.core :refer [>defn ? =>]]
            [muuntaja.core :as m]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config])
  (:import (java.time Instant LocalDateTime ZoneOffset)
           (java.time.temporal ChronoUnit TemporalUnit)))

(>defn now-minus-days
  "Returns an instant that represents the current date minus some days. Assumes systemDefault timezone."
  [days]
  [int? :ret inst?]
  (.minus (Instant/now) ^Long days ^TemporalUnit ChronoUnit/DAYS))

(>defn now-plus-days-instant
  "Adds a number of days to the current datetime and then converts that to an instant."
  [days]
  [integer? :ret inst?]
  (.toInstant (.plusDays (LocalDateTime/now) days) ZoneOffset/UTC))

(>defn ascending
  "Comparator, can be used to sort collections in an ascending way."
  [a b]
  [any? any? :ret number?]
  (compare b a))

(>defn build-allowed-origin
  "Build regular expressions, which define the allowed origins for API requests."
  [domain]
  [string? :ret any?]
  (when-let [[domain-name tld] (string/split domain #"\.")]
    (let [regex (if tld
                  "^((https?://)?(.*\\.)?(%s\\.%s))($|/.*$)"
                  "^((https?://)?(.*\\.)?(%s))($|/.*$)")]
      (re-pattern (format regex domain-name tld)))))

(def synonyms-german (atom {}))

;; Excellent macro copied from user kotarak: https://stackoverflow.com/a/1879961/552491
(defn try-times*
  "Executes thunk. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n thunk]
  (loop [n n]
    (if-let [result (try
                      [(thunk)]
                      (catch Exception e
                        (when (zero? n)
                          (throw e))))]
      (result 0)
      (recur (dec n)))))

(defmacro try-times
  "Executes body. If an exception is thrown, will retry. At most n retries
  are done. If still some exception is thrown it is bubbled upwards in
  the call chain."
  [n & body]
  `(try-times* ~n (fn [] ~@body)))

(>defn post-in-mattermost!
  "Post a message via webhook in our mattermost. Optional `channel` must be the
  slug of a channel name, e.g. `gitlabs-dirty-secrets`. Defaults to the
  configuration set in mattermost."
  ([message]
   [string? => map?]
   (post-in-mattermost! message nil))
  ([message channel]
   [string? (? string?) => map?]
   (let [body-ready (cond-> {:text message}
                      channel (assoc :channel channel))]
     (client/post
      config/mattermost-webhook-url
      {:body (m/encode "application/json" body-ready)
       :content-type :json
       :accept :json}))))

(>defn post-error-in-chat
  "Takes the service's name and the error message to leave a message in our
  mattermost."
  [service message]
  [string? string? => map?]
  (when shared-config/production?
    (post-in-mattermost! (format "[💥 %s] %s" service message) "gitlabs-dirty-secrets")))
