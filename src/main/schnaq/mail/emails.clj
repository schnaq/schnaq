(ns schnaq.mail.emails
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as cstring]
            [ghostwheel.core :refer [>defn >defn- ?]]
            [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [schnaq.mail.template :as template]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(def ^:private conn {:host (:sender-host config/email)
                     :ssl true
                     :user (:sender-address config/email)
                     :pass (:sender-password config/email)})

(def ^:private mail-configured?
  (every? not-empty (vals config/email)))

(>defn- valid-mail
  "Check valid mail"
  [mail]
  [string? :ret (? string?)]
  (if (re-matches #"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,10}" mail)
    mail
    (log/info (format "Mail validation failed for address %s" mail))))

(def ^:private failed-sendings (atom '()))

(>defn- send-mail-with-custom-body
  "Sends a single mail to a recipient with a passed body."
  [title recipient body]
  [string? string? coll? :ret (? coll?)]
  (if mail-configured?
    (if (valid-mail recipient)
      (try
        (send-message conn {:from (:sender-address config/email)
                            :to recipient
                            :subject title
                            :body body})
        (log/info "Sent mail to" recipient)
        (Thread/sleep 100)
        (catch Exception exception
          (log/error "Failed to send mail to" recipient)
          (log/error exception)
          (swap! failed-sendings conj recipient)))
      (swap! failed-sendings conj recipient))
    (log/info "Should send an email now, but email is not configured.")))

(>defn send-mail
  "Sends a single mail to the recipient. Title and content are used as passed."
  ([title content recipient]
   [string? string? string? :ret (? coll?)]
   (send-mail-with-custom-body title recipient (template/mail "" title "" content "" "")))
  ([mail-title header title content recipient]
   [string? string? string? string? string? :ret (? coll?)]
   (send-mail-with-custom-body mail-title recipient (template/mail header title "" content "" "")))
  ([mail-title header title sub-title content additional-html-content additional-plain-content recipient]
   [string? string? string? string? string? string? string? string? :ret (? coll?)]
   (send-mail-with-custom-body mail-title recipient
                               (template/mail header
                                              title
                                              sub-title
                                              content
                                              additional-html-content
                                              additional-plain-content))))

(>defn send-mails
  "Sends an email with a `title` and `content` to all valid recipients.
  Returns a list of invalid addresses and failed sends."
  ([title content recipients]
   [string? string? (s/coll-of string?) :ret any?]
   (reset! failed-sendings '())
   (run! (partial send-mail title content) recipients)
   {:failed-sendings @failed-sendings}))

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
        (log/info "Sent" email-type "mail to" recipient)
        :ok
        (catch Exception exception
          (log/error "Failed to send" email-type "mail to" recipient)
          (log/error exception)))
      (log/error "Recipient's mail address is invalid: " recipient))))

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
