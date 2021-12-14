(ns schnaq.notification-service.core
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.mail.emails :as emails]
            [schnaq.notification-service.mail-builder :as mail-builder]
            [schnaq.notification-service.schedule :as schedule]
            [taoensso.timbre :as log]))

(s/def ::discussions-with-new-statements (s/coll-of ::specs/discussion))
(s/def ::user-with-changed-discussions
  (s/merge ::specs/registered-user (s/keys :req-un [::discussions-with-new-statements])))

(>defn- discussions-with-new-statements-in-interval
  "Query all discussions of users respecting their notification interval. Query
  for these discussions all new statements in the time between now and the
  timestamp and create a map to query these results."
  [timestamp interval]
  [inst? :user.registered/notification-mail-interval :ret (s/map-of :discussion/share-hash ::specs/discussion)]
  (let [subscribed-discussions (discussion-db/discussions-by-share-hashes (user-db/subscribed-share-hashes interval))
        discussions (discussion-db/discussions-with-new-statements
                     subscribed-discussions timestamp)]
    (into {} (map (juxt :discussion/share-hash identity) discussions))))

(>defn- assoc-discussions-with-new-statements
  "Assoc all subscribed discussions to a user. Adds a new field 
  `:discussions-with-new-statements` containing all subscribed discussions,
   which received new statements."
  [discussions-with-new-statements user]
  [::discussions-with-new-statements ::specs/registered-user :ret ::user-with-changed-discussions]
  (assoc user :discussions-with-new-statements
         (remove nil?
                 (map #(get discussions-with-new-statements (:discussion/share-hash %))
                      (:user.registered/visited-schnaqs user)))))

(>defn- users-with-changed-discussions
  "Assoc to all users those discussions, with received new statements between 
  now and the timestamp."
  [timestamp interval]
  [inst? :user.registered/notification-mail-interval :ret (s/coll-of ::user-with-changed-discussions)]
  (let [users (user-db/users-by-notification-interval interval)
        changed-discussions (discussions-with-new-statements-in-interval timestamp interval)]
    (remove #(empty? (:discussions-with-new-statements %))
            (map (partial assoc-discussions-with-new-statements changed-discussions)
                 users))))

;; -----------------------------------------------------------------------------

(>defn- send-schnaq-diffs
  "Build and send a mail containing links to each schnaq with new statements."
  [{:user.registered/keys [keycloak-id email] :as user}]
  [::users-with-changed-discussions :ret nil?]
  (let [total-new-statements (->> user :discussions-with-new-statements (map :new-statements) (apply +))
        new-statements-content-html (mail-builder/build-new-statements-html user)
        new-statements-content-plain (mail-builder/build-new-statements-plain user)
        personal-greeting (mail-builder/build-personal-greeting user)
        new-statements-greeting (mail-builder/build-number-unseen-statements total-new-statements)]
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

(>defn- start-mail-schedule
  "Takes a core.async channel-atom containing the instances of the next dates 
   when mails should be sent and an interval to pre-select the users.
   
   Infinitely loops and sends regularly mails."
  [channel timestamp interval]
  [any? inst? :user.registered/notification-mail-interval :ret nil?]
  (log/info "Starting mail schedule for" interval)
  (go-loop []
    (when-let [current-time (<! @channel)]
      (log/info "Sending new mails, timestamp" current-time)
      (run! send-schnaq-diffs (users-with-changed-discussions timestamp interval))
      (recur))))

;; -----------------------------------------------------------------------------

(defn -main
  [& _args]
  (log/info "Initializing mail notification service")
  (when (main-db/connection-possible?)
    (start-mail-schedule schedule/daily (main-db/days-ago 1) :notification-mail-interval/daily)
    (start-mail-schedule schedule/weekly (main-db/days-ago 7) :notification-mail-interval/weekly)))
