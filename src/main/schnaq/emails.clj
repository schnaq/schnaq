(ns schnaq.emails
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [schnaq.translations :refer [email-templates]]
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

(>defn- send-mail
  "Sends a single mail to the recipient. Title and content are used as passed."
  [title content recipient]
  [string? string? string? :ret (? coll?)]
  (if (valid-mail recipient)
    (try
      (send-message conn {:from (:sender-address config/email)
                          :to recipient
                          :subject title
                          :body [{:type "text/plain; charset=utf-8"
                                  :content content}]})
      (info "Sent mail to" recipient)
      (Thread/sleep 100)
      (catch Exception exception
        (error "Failed to send mail to" recipient)
        (error exception)
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

(>defn send-html-mail
  "Sends a html mail and an alternative text version to any contact. The html-template should be a url or file-path
  usable by slurp. Title and body are keys for the email-templates map."
  [recipient title text-body html-template-path email-type]
  [string? keyword? keyword? string? any? :ret any?]
  (if (valid-mail recipient)
    (try
      (send-message conn {:from (:sender-address config/email)
                          :to recipient
                          :subject (email-templates title)
                          :body [:alternative
                                 {:type "text/plain; charset=utf-8" :content (email-templates text-body)}
                                 {:type "text/html; charset=utf-8" :content (slurp html-template-path)}]})
      (info "Sent" email-type "mail to" recipient)
      (catch Exception exception
        (error "Failed to send" email-type "mail to" recipient)
        (error exception)))
    (error "Recipient's mail address is invalid: " recipient)))

(>defn send-welcome-mail
  "Sends a welcome e-mail to a recipient. The mail template is stored in s3."
  [recipient]
  [string? :ret any?]
  (send-html-mail recipient :welcome/title :welcome/body
                  "https://s3.disqtec.com/welcome-mail/welcome_template.html"
                  "welcome"))
