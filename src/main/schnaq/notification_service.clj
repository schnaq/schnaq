(ns schnaq.notification-service
  (:require [chime.core :as chime-core]
            [hiccup.util :as hiccup-util]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.links :as schnaq-links]
            [schnaq.mail.emails :as emails]
            [schnaq.mail.template :as template]
            [taoensso.timbre :as log])
  (:import (java.time LocalTime ZonedDateTime ZoneId Period)))

(defonce mail-update-schedule (atom nil))

(defn- build-discussion-diff-list
  "Build a map of discussion hashes with new statements as values"
  [user-keycloak-id discussion-hashes]
  (reduce conj
          (map (fn [discussion-hash]
                 {discussion-hash (discussion-db/new-statement-ids-for-user
                                    user-keycloak-id discussion-hash)})
               discussion-hashes)))

(defn- build-new-statements-content
  "Additional html content to display the number of new statements and a navigation button
  to the corresponding schnaq. This functions maps over all schnaqs."
  [new-statements-per-schnaq]
  (reduce
    str
    (map (fn [[discussion-hash statements]]
           (let [number-statements (count statements)
                 discussion (discussion-db/discussion-by-share-hash discussion-hash)
                 discussion-title (hiccup-util/escape-html (:discussion/title discussion))
                 new-statements-text (if (= 1 number-statements)
                                       (str number-statements " neuer Beitrag")
                                       (str number-statements " neue Beiträge"))
                 button-text "Zum schnaq"]
             (when-not (zero? number-statements)
               (template/mail-content-left-button-right-template
                 discussion-title
                 new-statements-text
                 button-text
                 (schnaq-links/get-share-link discussion-hash)))))
         new-statements-per-schnaq)))

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
        new-statements-per-schnaq (build-discussion-diff-list user-keycloak-id
                                                              discussion-hashes)
        total-new-statements (reduce + (map (fn [[_ news]] (count news)) new-statements-per-schnaq))
        new-statements-content (build-new-statements-content new-statements-per-schnaq)
        personal-greeting (build-personal-greetings user)
        new-statements-greeting (build-number-unseen-statements total-new-statements)]
    (log/info "User" user-keycloak-id "has" total-new-statements "unread statements")
    (when-not (zero? total-new-statements)
      (emails/send-mail-with-body
        "Neuigkeiten aus deinen schnaqs"
        email
        (template/mail-template "Neuigkeiten aus deinen schnaqs"
                                personal-greeting
                                new-statements-greeting
                                ""
                                new-statements-content)))))

(defn- send-all-users-schnaq-updates []
  (let [all-users (user-db/all-registered-users)]
    (doseq [user all-users]
      (send-schnaq-diffs user))))

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
            (chime-schedule (LocalTime/of 7 0 0) (send-all-users-schnaq-updates)))))

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