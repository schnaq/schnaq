(ns schnaq.emails
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as cstring]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :refer [info error]])
  (:import (java.util UUID)))

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
  usable by slurp. Title and body are keys for the email-templates map. Format-map is used to substitute things
  in the text-body and html template. The keys are substituted by their values."
  [recipient title text-body html-template-path email-type format-map]
  [string? keyword? keyword? string? any? map? :ret any?]
  (let [replace-fn #(cstring/replace %1 (first %2) (second %2))]
    (if (valid-mail recipient)
      (try
        (send-message conn {:from (:sender-address config/email)
                            :to recipient
                            :subject (email-templates title)
                            :body [:alternative
                                   {:type "text/plain; charset=utf-8" :content
                                    (reduce replace-fn (email-templates text-body) format-map)}
                                   {:type "text/html; charset=utf-8" :content
                                    (reduce replace-fn (slurp html-template-path) format-map)}]})
        (info "Sent" email-type "mail to" recipient)
        :ok
        (catch Exception exception
          (error "Failed to send" email-type "mail to" recipient)
          (error exception)))
      (error "Recipient's mail address is invalid: " recipient))))

(>defn send-welcome-mail
  "Sends a welcome e-mail to a recipient. The mail template is stored in s3."
  [recipient]
  [string? :ret any?]
  (send-html-mail recipient :welcome/title :welcome/body
                  "https://s3.disqtec.com/welcome-mail/welcome_template.html"
                  "welcome" {}))

(>defn send-remote-work-lead-magnet
  "Sends the lead magnet pdf to a recipient. The mail template is stored in s3."
  [recipient]
  [string? :ret any?]
  (send-html-mail recipient :lead-magnet/title :lead-magnet/body
                  "https://s3.disqtec.com/email/lead-magnet/dsgvo-check-mail.html"
                  "lead-magnet remote work"
                  {"$DOWNLOAD_LINK"
                   (str "https://s3.disqtec.com/downloads/Datenschutzkonform_arbeiten_schnaq.com.pdf?key="
                        (.toString (UUID/randomUUID)))}))
