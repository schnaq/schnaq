(ns schnaq.notification-service.core
  (:require [clojure.core.async :refer [go-loop <!]]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as main-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.mail.emails :as emails]
            [schnaq.notification-service.mail-builder :as mail-builder]
            [schnaq.notification-service.schedule :as schedule]
            [schnaq.notification-service.specs]
            [taoensso.timbre :as log]))

(>defn- discussions-with-new-statements-in-interval
  "Query all discussions of users respecting their notification interval. Query
  for these discussions all new statements in the time between now and the
  timestamp and create a map to query these results."
  [timestamp interval]
  [inst? :user.registered/notification-mail-interval => :notification-service/share-hash-to-discussion]
  (let [subscribed-discussions (discussion-db/discussions-by-share-hashes (user-db/subscribed-share-hashes interval))
        discussions (discussion-db/discussions-with-new-statements
                     subscribed-discussions timestamp)]
    (into {} (map (juxt :discussion/share-hash identity) discussions))))

(>defn- remove-discussions-with-no-other-users
  "Remove those discussions, where the user is the only author of newly created
  statements."
  [discussions-with-new-statements user-id]
  [:notification-service/share-hash-to-discussion :db/id => :notification-service/share-hash-to-discussion]
  (->> (seq discussions-with-new-statements)
       (remove (fn [[_share-hash discussion]]
                 (let [authors (get-in discussion [:new-statements :authors])]
                   (and (= 1 (count authors))
                        (authors user-id)))))
       (into {})))

(>defn- assoc-discussions-with-new-statements
  "Assoc all subscribed discussions to a user. Adds a new field 
  `:discussions-with-new-statements` containing all subscribed discussions,
   which received new statements."
  [discussions-with-new-statements user]
  [:notification-service/share-hash-to-discussion ::specs/registered-user => :notification-service/user-with-changed-discussions]
  (let [discussions (remove-discussions-with-no-other-users discussions-with-new-statements (:db/id user))]
    (->> user
         :user.registered/visited-schnaqs
         (map #(get discussions (:discussion/share-hash %)))
         (remove nil?)
         (assoc user :discussions-with-new-statements))))

(>defn- users-with-changed-discussions
  "Assoc to all users those discussions, with received new statements between 
  now and the timestamp."
  [timestamp interval]
  [inst? :user.registered/notification-mail-interval => (s/coll-of :notification-service/user-with-changed-discussions)]
  (let [users (user-db/users-by-notification-interval interval)
        changed-discussions (discussions-with-new-statements-in-interval timestamp interval)]
    (remove #(empty? (:discussions-with-new-statements %))
            (map (partial assoc-discussions-with-new-statements changed-discussions)
                 users))))

;; -----------------------------------------------------------------------------

(>defn- send-schnaq-diffs
  "Build and send a mail containing links to each schnaq with new statements."
  [{:user.registered/keys [keycloak-id email] :as user}]
  [:notification-service/user-with-changed-discussions => nil?]
  (log/info "send-schnaq-diffs")
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
  [channel time-fn interval]
  [any? fn? :user.registered/notification-mail-interval => any?]
  (log/info "Starting mail schedule for" interval)
  (go-loop []
    (when-let [_current-time (<! @channel)]
      (log/info (format "Sending new mails [%s]" interval))
      (run! send-schnaq-diffs (users-with-changed-discussions (time-fn) interval))
      (recur))))

;; -----------------------------------------------------------------------------

(defn -main
  [& _args]
  (log/info "Initializing mail notification service")
  (when (main-db/connection-possible?)
    (start-mail-schedule schedule/every-minute #(main-db/minutes-ago 1) :notification-mail-interval/every-minute)
    (start-mail-schedule schedule/daily #(main-db/days-ago 1) :notification-mail-interval/daily)
    (start-mail-schedule schedule/weekly #(main-db/days-ago 7) :notification-mail-interval/weekly)))

(comment

  (def data {"fbc512d7-89d1-4f6c-be6a-fc05a44b2976"
             {:db/id 17592186049507
              :discussion/title "123"
              :discussion/share-hash "fbc512d7-89d1-4f6c-be6a-fc05a44b2976"
              :discussion/created-at #inst "2022-01-16T20:08:48.741-00:00"
              :discussion/author
              {:db/id 17592186045435
               :user.registered/keycloak-id "d6d8a351-2074-46ff-aa9b-9c57ab6c6a18"
               :user.registered/display-name "n2o"}
              :new-statements {:total 15, :authors #{17592186045435}}}})

  (users-with-changed-discussions (main-db/minutes-ago 152) :notification-mail-interval/daily)

  (remove-discussions-with-no-other-users data 17592186045435)

  nil)
