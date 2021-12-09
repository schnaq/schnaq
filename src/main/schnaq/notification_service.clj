(ns schnaq.notification-service
  (:require [chime.core :as chime-core]
            [chime.core-async :refer [chime-ch]]
            [clojure.core.async :refer [go-loop <!]]
            [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [hiccup.util :as hiccup-util]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.links :as schnaq-links]
            [schnaq.mail.emails :as emails]
            [schnaq.mail.template :as template]
            [taoensso.timbre :as log])
  (:import (java.time LocalTime LocalDate LocalDateTime ZonedDateTime ZoneId Period DayOfWeek Instant Duration)
           (java.time.temporal ChronoUnit TemporalAdjusters)))

(s/def :time/zoned-date-time (partial instance? ZonedDateTime))

(def ^:private start-next-morning
  "ZonedDateTime to indicate the start, today at 7 o'clock in UTC, which is 
   6 o'clock in Europe/Berlin."
  (-> (LocalDateTime/of (LocalDate/now) (LocalTime/of 7 0))
      (.adjustInto (ZonedDateTime/now (ZoneId/of (:timezone shared-config/time-settings))))))

(defn- timestamp-next-monday
  "Takes a `ZonedDateTime` and returns the date of the next monday."
  [timestamp]
  [:time/zoned-date-time :ret :time/zoned-date-time]
  (let [timestamp-next-monday (-> timestamp (.with (TemporalAdjusters/nextOrSame DayOfWeek/MONDAY)))
        days-left (.between ChronoUnit/DAYS timestamp timestamp-next-monday)]
    (.plusDays timestamp days-left)))

(>defn- create-schedule
  "Create an infinite collection with instances, re-occurring depending on the 
   `days`-parameter starting at `timestamp`."
  [timestamp days]
  [:time/zoned-date-time pos-int? :ret (s/coll-of inst?)]
  (chime-core/periodic-seq (.toInstant timestamp) (Period/ofDays days)))

(def ^:private daily
  "Create a core.async channel containing a list of instances, repeating every
   day. Drops first element in list to prevent direct-sending mail."
  (atom
   (chime-ch (rest (create-schedule start-next-morning 1)))))

(def ^:private weekly
  "Same as `daily`, but for a weekly schedule."
  (atom
   (chime-ch (create-schedule (timestamp-next-monday start-next-morning) 7))))

;; -----------------------------------------------------------------------------
;; Generate and style mail

(>defn- build-new-statements-content
  "Additional content to display the number of new statements and a navigation button
  to the corresponding schnaq. This functions maps over all schnaqs."
  [new-statements-per-discussion content-fn]
  [::specs/share-hash-statement-id-mapping fn? :ret string?]
  (reduce
   str
   (map (fn [[discussion-hash statements]]
          (let [number-statements (count statements)
                discussion (discussion-db/discussion-by-share-hash discussion-hash)
                discussion-title (hiccup-util/escape-html (:discussion/title discussion))
                new-statements-text (if (= 1 number-statements)
                                      (str number-statements " neuer Beitrag")
                                      (str number-statements " neue Beiträge"))]
            (when-not (zero? number-statements)
              (content-fn discussion-title new-statements-text discussion-hash))))
        new-statements-per-discussion)))

(>defn- build-new-statements-html
  "New statements info as html. Preparation for sending it via mail."
  [new-statements-per-discussion]
  [::specs/share-hash-statement-id-mapping :ret string?]
  (build-new-statements-content
   new-statements-per-discussion
   (fn [title text discussion-hash]
     (template/mail-content-left-button-right
      title text "Zum schnaq" (schnaq-links/get-share-link discussion-hash)))))

(>defn- build-new-statements-plain
  "New statements info as plain text. Preparation for a standard mail without 
   HTML."
  [new-statements-per-discussion]
  [::specs/share-hash-statement-id-mapping :ret string?]
  (build-new-statements-content
   new-statements-per-discussion
   (fn [title text discussion-hash]
     (format "%s in %s: %s\n" text title (schnaq-links/get-share-link discussion-hash)))))

(defn- build-personal-greetings
  "Takes the user's display name and creates a salutation."
  [{:user.registered/keys [display-name]}]
  (format "Hallo %s," (hiccup-util/escape-html display-name)))

(>defn- build-number-unseen-statements
  "Sum up all new statements over all discussions and put the sum in a text 
   body."
  [total-new-statements]
  [nat-int? :ret string?]
  (let [statements-text (if (= 1 total-new-statements)
                          "einen neuen Beitrag"
                          (str total-new-statements " neue Beiträge"))]
    (format "es gibt %s in deinen besuchten schnaqs!" statements-text)))

(>defn- send-schnaq-diffs
  "Build and send a mail containing links to each schnaq with new statements."
  [{:user.registered/keys [keycloak-id email] :as user}]
  [::specs/registered-user :ret nil?]
  (let [new-statements-per-discussion (discussion-db/new-statements-by-discussion-hash user)
        total-new-statements (reduce + (map (fn [[_ news]] (count news)) new-statements-per-discussion))
        new-statements-content-html (build-new-statements-html new-statements-per-discussion)
        new-statements-content-plain (build-new-statements-plain new-statements-per-discussion)
        personal-greeting (build-personal-greetings user)
        new-statements-greeting (build-number-unseen-statements total-new-statements)]
    (when-not (zero? total-new-statements)
      (log/info (format "User %s has %d unread statements" keycloak-id total-new-statements))
      (emails/send-mail "Neuigkeiten aus deinen schnaqs"
                        "Neuigkeiten aus deinen schnaqs"
                        personal-greeting
                        new-statements-greeting
                        ""
                        new-statements-content-html
                        new-statements-content-plain
                        email))))

;; -----------------------------------------------------------------------------
;; Mail sending

(>defn- start-mail-schedule
  "Takes a core.async channel-atom containing the instances of the next dates 
   when mails should be sent and an interval to pre-select the users.
   
   Infinitely loops and sends regularly mails."
  [channel interval]
  [any? :user.registered/notification-mail-interval :ret nil?]
  (go-loop []
    (when-let [timestamp (<! @channel)]
      (log/info "Sending new mails, timestamp" timestamp)
      (send-schnaq-diffs (first (user-db/users-by-notification-interval interval)))
      (recur))))

;; -----------------------------------------------------------------------------

(defn -main
  [& _args]
  (start-mail-schedule daily :notification-mail-interval/daily)
  (start-mail-schedule weekly :notification-mail-interval/weekly))

;; -----------------------------------------------------------------------------

(comment

  "Create dev-channel to test application. Sends mails every minute."
  (def dev-channel (atom nil))
  (reset! dev-channel
          (chime-ch (rest (chime-core/periodic-seq (Instant/now) (Duration/ofMinutes 1)))))

  (reset! dev-channel nil)

  (start-mail-schedule dev-channel :notification-mail-interval/daily)

  nil)
