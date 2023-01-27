(ns schnaq.api.moderation
  ;; TODO move the owner route here as well
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.main :as db]
            [schnaq.database.patterns :as patterns]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.mail.emails :as emails]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :as log]))

(>defn- promote-user-to-moderator
  "Send URL to admin-center via mail to recipient."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash recipient admin-center]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (log/debug "Promote user " recipient " to moderator.")
    (user-db/promote-user-to-moderator share-hash recipient)
    (log/debug "Send admin link for discussion " discussion-title " via E-Mail")
    (ok (merge
         {:message "Email sent successfully"}
         (emails/send-mails
          (format (email-templates :admin-center/title) discussion-title)
          (format (email-templates :admin-center/body) discussion-title admin-center)
          [recipient])))))
;; TODO add test
(defn- load-moderators
  "Sends a list of all moderator emails to the frontend. Can only be queried by a moderator."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])
        schnaq-moderators (db/fast-pull [:discussion/share-hash share-hash]
                                        [{:discussion/moderators [:user.registered/email]}])]
    (ok {:moderators (map :user.registered/email (:discussion/moderators schnaq-moderators))})))

(defn- demote-moderator
  ;; TODO write tests
  "Removes a moderator from the schnaq, if the requester is the owner."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash email]} (:body parameters)
        requesting-user (:db/id (db/fast-pull [:user.registered/keycloak-id (:sub identity)] [:db/id]))
        modified-discussion-author (get-in (db/fast-pull [:discussion/share-hash share-hash] patterns/discussion)
                                           [:discussion/author :db/id])]
    (if (= requesting-user modified-discussion-author)
      (do
        (user-db/demote-moderator share-hash email)
        (ok {:demoted? true}))
      (bad-request (at/build-error-body :not-author "Only the author can demote users.")))))

;; -----------------------------------------------------------------------------

(def moderation-routes
  ["/moderation" {:swagger {:tags ["moderation"]}
                  :middleware [:discussion/user-moderator?]
                  :responses {403 at/response-error-body}}
   ["/promote-user" {:name :api.moderation/promote-user
                     :post promote-user-to-moderator
                     :description (at/get-doc #'promote-user-to-moderator)
                     :parameters {:body {:share-hash :discussion/share-hash
                                         :recipient string?
                                         :admin-center string?}}
                     :responses {200 {:body {:message string?
                                             :failed-sendings (s/coll-of string?)}}}}]
   ["/moderators" {:get load-moderators
                   :description (at/get-doc #'load-moderators)
                   :name :api.moderation/moderators
                   :parameters {:query {:share-hash :discussion/share-hash}}
                   :responses {200 {:body {:moderators (s/coll-of ::specs/email)}}}}]
   ["/demote" {:post demote-moderator
               :description (at/get-doc #'demote-moderator)
               :name :api.schnaq.owner/demote-moderator
               :middleware [:discussion/valid-share-hash? :user/authenticated?]
               :parameters {:body {:share-hash :discussion/share-hash
                                   :email ::specs/email}}
               :responses {200 {:body {:demoted? boolean?}}
                           400 at/response-error-body}}]])
