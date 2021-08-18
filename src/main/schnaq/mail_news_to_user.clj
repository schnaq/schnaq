(ns schnaq.mail-news-to-user
  (:require [hiccup.core :refer [html]]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as emails]))


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

(defn send-schnaq-diffs [user-keycloak-id]
  (let [user (fast-pull [:user.registered/keycloak-id user-keycloak-id]
                        user-db/registered-private-user-pattern)
        email (:user.registered/email user)
        discussion-hashes (map #(:discussion/share-hash %)
                               (:user.registered/visited-schnaqs user))
        new-statements-per-schnaq (build-discussion-diff-list user-keycloak-id
                                                              discussion-hashes)
        new-statements-content (build-new-statements-content new-statements-per-schnaq)
        personal-greeting (build-personal-greetings user-keycloak-id)]
    (emails/send-mail "Neuigkeiten aus deinen schnaqs"
                      (html [:div
                             personal-greeting
                             new-statements-content])
                      email "text/html")))