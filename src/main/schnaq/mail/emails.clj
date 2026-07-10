(ns schnaq.mail.emails
  "Handle sending emails to users."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ?]]
            [hiccup.util :as hiccup-util]
            [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [schnaq.database.specs :as specs]
            [schnaq.links :as schnaq-links]
            [schnaq.mail.template :as template]
            [taoensso.timbre :as log]))

(def ^:private email-config->env-var
  {:sender-address "EMAIL_SENDER_ADDRESS"
   :sender-host "EMAIL_HOST"
   :sender-username "EMAIL_USERNAME"
   :sender-password "EMAIL_PASSWORD"})

(defn missing-email-config-keys
  "Return env var names for unset email configuration values."
  []
  (->> config/email
       (filter (fn [[_ v]] (empty? v)))
       (map (fn [[k _]] (get email-config->env-var k (name k))))
       vec))

(defn mail-configured?
  "True when all required email configuration values are set."
  []
  (empty? (missing-email-config-keys)))

(def ^:private conn {:host (:sender-host config/email)
                     :port (:sender-port config/email)
                     :ssl true
                     :user (:sender-username config/email)
                     :pass (:sender-password config/email)})

(def ^:private missing-config-logged? (atom false))

(defn- log-missing-email-config-once!
  []
  (when (compare-and-set! missing-config-logged? false true)
    (log/warn "E-Mail not configured. Missing environment variables:"
              (str/join ", " (missing-email-config-keys)))))

(>defn- valid-mail
  "Check valid mail"
  [mail]
  [string? :ret (? string?)]
  (if (re-matches #"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,10}" mail)
    mail
    (log/info (format "Mail validation failed for address %s" mail))))

(def ^:private failed-sendings (atom '()))

(defn- postal-send-success?
  [result]
  (zero? (:code result 99)))

(>defn- send-mail-with-custom-body
  "Sends a single mail to a recipient with a passed body."
  [title recipient body]
  [string? string? coll? :ret (? coll?)]
  (if (mail-configured?)
    (if (valid-mail recipient)
      (try
        (let [result (send-message conn {:from (:sender-address config/email)
                                         :to recipient
                                         :subject title
                                         :body body})]
          (if (postal-send-success? result)
            (do
              (log/info "Sent mail to" recipient)
              (Thread/sleep 100))
            (do
              (log/error "Failed to send mail to" recipient "postal returned" result)
              (swap! failed-sendings conj recipient))))
        (catch Exception exception
          (log/error "Failed to send mail to" recipient)
          (log/error exception)
          (swap! failed-sendings conj recipient)))
      (swap! failed-sendings conj recipient))
    (do
      (log-missing-email-config-once!)
      (log/info (format "Should send an email to %s now, but email is not configured." recipient)))))

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
  [title content recipients]
  [string? string? (s/coll-of string?) :ret any?]
  (reset! failed-sendings '())
  (run! (partial send-mail title content) recipients)
  {:failed-sendings @failed-sendings})

(>defn send-flagged-post
  "Send a mail containing the content and link to the flagged statement "
  [discussion statement recipients]
  [::specs/discussion ::specs/statement (s/coll-of string?) :ret any?]
  (send-mails
   "[Aktion erforderlich] Beitrag mit bedenklichem Inhalt!"
   (format "Diese Mail erhalten die schnaq Administrator:innen und der:die Autor:in des schnaqs.\n\nEin:e Nutzer:in hat folgenden Beitrag als Bedenklich gemeldet:\n\n\"%s\"\n\n %s"
           (hiccup-util/escape-html (:statement/content statement))
           (schnaq-links/get-link-to-statement
            (:discussion/share-hash discussion)
            (:db/id statement)))
   recipients))
