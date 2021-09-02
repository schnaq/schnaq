(ns schnaq.notification-service
  (:require [chime.core :as chime-core]
            [hiccup.util :as hiccup-util]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.links :as schnaq-links]
            [schnaq.mail.emails :as emails]
            [schnaq.mail.template :as template]
            [taoensso.timbre :as log])
  (:import (java.time LocalTime ZonedDateTime ZoneId Period DayOfWeek Instant)))

(defonce mail-update-schedule (atom nil))

(defn- build-new-statements-content
  "Additional content to display the number of new statements and a navigation button
  to the corresponding schnaq. This functions maps over all schnaqs."
  [new-statements-per-schnaq content-fn]
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
         new-statements-per-schnaq)))

(defn- build-new-statements-html
  "New statements info as html"
  [new-statements-per-schnaq]
  (build-new-statements-content
    new-statements-per-schnaq
    (fn [title text discussion-hash]
      (template/mail-content-left-button-right
        title text "Zum schnaq" (schnaq-links/get-share-link discussion-hash)))))

(defn- build-new-statements-plain
  "New statements info as plain text"
  [new-statements-per-schnaq]
  (build-new-statements-content
    new-statements-per-schnaq
    (fn [title text discussion-hash]
      (str text " in " title ": " (schnaq-links/get-share-link discussion-hash) "\n"))))

(defn- build-personal-greetings [user]
  (str "Hallo " (hiccup-util/escape-html (:user.registered/display-name user)) ","))

(defn- build-number-unseen-statements [total-new-statements]
  (let [statements-text (if (= 1 total-new-statements)
                          "einen neuen Beitrag"
                          (str total-new-statements " neue Beiträge"))]

    (str "es gibt " statements-text " in deinen besuchten schnaqs!")))

(defn- send-schnaq-diffs
  "Build and send a mail containing links to each schnaq with new statements."
  [user]
  (let [user-keycloak-id (:user.registered/keycloak-id user)
        email (:user.registered/email user)
        discussion-hashes (map :discussion/share-hash (:user.registered/visited-schnaqs user))
        new-statements-per-schnaq (discussion-db/build-discussion-diff-list
                                    user-keycloak-id
                                    discussion-hashes)
        total-new-statements (reduce + (map (fn [[_ news]] (count news)) new-statements-per-schnaq))
        new-statements-content-html (build-new-statements-html new-statements-per-schnaq)
        new-statements-content-plain (build-new-statements-plain new-statements-per-schnaq)
        personal-greeting (build-personal-greetings user)
        new-statements-greeting (build-number-unseen-statements total-new-statements)]
    (log/info "User" user-keycloak-id "has" total-new-statements "unread statements")
    (when-not (zero? total-new-statements)
      (emails/send-mail "Neuigkeiten aus deinen schnaqs"
                        "Neuigkeiten aus deinen schnaqs"
                        personal-greeting
                        new-statements-greeting
                        ""
                        new-statements-content-html
                        new-statements-content-plain
                        email))))

(defn check-notification-interval?
  "Checks if the user specified notification time interval has passed.
  True if it's Monday for /weekly, false for /never and true as default."
  [user time]
  (let [interval (:user.registered/notification-mail-interval user)]
    (case interval
      :notification-mail-interval/never
      false
      :notification-mail-interval/weekly
      (= DayOfWeek/MONDAY (-> time (.atZone (ZoneId/of config/time-zone)) (.getDayOfWeek)))
      true)))

(defn- send-all-users-schnaq-updates [time]
  (let [all-users (user-db/all-registered-users)]
    (doseq [user all-users]
      (when (check-notification-interval? user time)
        (send-schnaq-diffs user)))))

(defn- chime-schedule
  "Chime periodic sequence to call a function once a day."
  [time-at timed-function]
  (chime-core/chime-at
    (chime-core/periodic-seq
      (-> time-at (.adjustInto (ZonedDateTime/now (ZoneId/of "Europe/Paris"))) .toInstant)
      (Period/ofDays 1))
    (fn [_time] (future timed-function))))

(defn start-mail-update-schedule
  "Start a schedule to send a mail to each user at ca. 7:00 AM with updates of their schnaqs."
  []
  (when (nil? @mail-update-schedule)
    (log/info "Starting mail schedule")
    (reset! mail-update-schedule
            (chime-schedule (LocalTime/of 7 0 0) #(send-all-users-schnaq-updates %)))))

(defn stop-mail-update-schedule
  "Close the mail schedule."
  []
  (when @mail-update-schedule
    (log/info "Closing mail schedule")
    (.close @mail-update-schedule)
    (reset! mail-update-schedule nil)))

(defn -main
  [& _args]
  (start-mail-update-schedule))

(comment
  "Send a notifiaction mail to all users"
  (send-all-users-schnaq-updates (Instant/now))
  :end)

