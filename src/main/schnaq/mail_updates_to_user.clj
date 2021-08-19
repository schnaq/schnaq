(ns schnaq.mail-updates-to-user
  (:require [chime.core :as chime-core]
            [hiccup.core :refer [html]]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as emails]
            [taoensso.timbre :as log])
  (:import (java.time LocalTime ZonedDateTime ZoneId Period)))

(defonce mail-update-schedule (atom nil))

(defn- build-discussion-diff-list [user-keycloak-id discussion-hashes]
  (reduce conj
          (map
            (fn [discussion-hash]
              (when-not (nil? discussion-hash)
                {discussion-hash (map #(:db/id %)
                                      (discussion-db/new-statements-for-user
                                        user-keycloak-id
                                        discussion-hash))}))
            discussion-hashes)))

(defn- create-hyperlink-to-discussion [discussion]
  (let [title (:discussion/title discussion)
        share-hash (:discussion/share-hash discussion)
        link (str config/frontend-url "/schnaq/" share-hash)]
    (html [:a {:href link} title])))

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

(defn- build-personal-greetings [user-keycloak-id]
  (let [user (fast-pull [:user.registered/keycloak-id user-keycloak-id]
                        user-db/registered-user-public-pattern)]
    (html [:div [:h1 "Neuigkeiten aus deinen schnaqs"]
           [:h4 "Hallo " (:user.registered/display-name user) ", "
            "es gibt neue Beiträge in deinen besuchten schnaqs!"]])))

(defn- send-schnaq-diffs [user-keycloak-id]
  (let [user (fast-pull [:user.registered/keycloak-id user-keycloak-id]
                        user-db/registered-private-user-pattern)
        email (:user.registered/email user)
        discussion-hashes (map #(:discussion/share-hash %)
                               (:user.registered/visited-schnaqs user))
        new-statements-per-schnaq (build-discussion-diff-list user-keycloak-id
                                                              discussion-hashes)
        total-new-statements (reduce + (map (fn [[_ news]] (count news)) new-statements-per-schnaq))
        new-statements-content (build-new-statements-content new-statements-per-schnaq)
        personal-greeting (build-personal-greetings user-keycloak-id)]
    (println total-new-statements)
    (when-not (zero? total-new-statements)
      (emails/send-mail "Neuigkeiten aus deinen schnaqs"
                        (html [:div
                               personal-greeting
                               new-statements-content])
                        email "text/html"))))

(defn- send-all-users-schnaq-updates []
  (let [all-users (user-db/all-registered-users)]
    (doseq [[keycloak-id] all-users]
      (Thread/sleep 1000)                                   ; Delay each mail by one second to avoid spam
      (send-schnaq-diffs keycloak-id))))

(defn start-mail-update-schedule
  "Start a schedule to send a mail to each user at ca. 7:00 AM with updates of their schnaqs."
  []
  (when (nil? @mail-update-schedule)
    (log/info "Starting mail schedule")
    (reset! mail-update-schedule
            (chime-core/chime-at
              (chime-core/periodic-seq (-> (LocalTime/of 7 0 0)
                                           (.adjustInto (ZonedDateTime/now (ZoneId/of "Europe/Paris"))) .toInstant)
                                       (Period/ofDays 1))
              (fn [_time]
                (println "Hello")
                (send-all-users-schnaq-updates))))))

(defn stop-mail-update-schedule
  "Close the mail schedule."
  []
  (when-not (nil? @mail-update-schedule)
    (log/info "Closing mail schedule")
    (.close @mail-update-schedule)
    (reset! mail-update-schedule nil)))