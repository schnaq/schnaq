(ns schnaq.emails
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [taoensso.timbre :refer [info error]]))

(def ^:private conn {:host (:sender-host config/email)
                     :ssl true
                     :user (:sender-address config/email)
                     :pass (:sender-password config/email)})

(>defn- valid-mail
  "Check valid mail"
  [mail]
  [string? :ret (? string?)]
  (if (re-matches #"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,10}" mail)
    mail
    (info (format "Mail validation failed for address %s" mail))))

(def ^:private failed-sendings (atom '()))

(>defn send-mail
  "Sends a single mail to the recipient. Title and content are used as passed."
  [title content recipient]
  [string? string? string? :ret coll?]
  (if (valid-mail recipient)
    (try
      (send-message conn {:from (:sender-address config/email)
                          :to recipient
                          :subject title
                          :body [{:type "text/plain; charset=utf-8"
                                  :content content}]})
      (info "Sent mail to" recipient)
      (Thread/sleep 100)
      (catch Exception _
        (error "Failed to send mail to" recipient)
        (swap! failed-sendings conj recipient)))
    (swap! failed-sendings conj recipient)))

(>defn send-mails
  "Sends an email with a `title` and `content` to all valid recipients.
  Returns a list of invalid addresses and failed sends."
  [title content recipients]
  [string? string? (s/coll-of string?) :ret any?]
  (reset! failed-sendings '())
  (run! (partial send-mail title content) recipients)
  {:failed-sendings @failed-sendings})
