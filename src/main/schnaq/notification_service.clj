(ns schnaq.notification-service
  (:require [chime.core :as chime-core]
            [hiccup.core :refer [html]]
            [hiccup.util :as hiccup-util]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as emails]
            [schnaq.links :as schnaq-links]
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

(defn- create-hyperlink-to-discussion [discussion]
  (let [title (:discussion/title discussion)
        share-hash (:discussion/share-hash discussion)
        link (schnaq-links/get-share-link share-hash)]
    (html [:a {:href link} (hiccup-util/escape-html title)])))

(defn- build-new-statements-content [new-statements-per-schnaq]
  (reduce
    str
    (map (fn [[discussion-hash statements]]
           (let [number-statements (count statements)
                 discussion (discussion-db/discussion-by-share-hash discussion-hash)]
             (when-not (zero? number-statements)
               (if (= 1 number-statements)
                 (html [:div number-statements " neuer Beitrag in: "
                        (create-hyperlink-to-discussion discussion)])
                 (html [:div number-statements " neue Beiträge in: "
                        (create-hyperlink-to-discussion discussion)])))))
         new-statements-per-schnaq)))

(defn- build-personal-greetings [user]
  (html [:div [:h1 "Neuigkeiten aus deinen schnaqs"]
         [:h4 "Hallo " (hiccup-util/escape-html (:user.registered/display-name user)) ", "
          "es gibt neue Beiträge in deinen besuchten schnaqs!"]]))

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
        personal-greeting (build-personal-greetings user)]
    (log/info "User" user-keycloak-id "has" total-new-statements "unread statements")
    (when-not (zero? total-new-statements)
      (emails/send-mail "Neuigkeiten aus deinen schnaqs"
                        (html [:div
                               personal-greeting
                               new-statements-content])
                        email "text/html"))))

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