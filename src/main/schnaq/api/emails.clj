(ns schnaq.api.emails
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs]
            [schnaq.mail.emails :as emails]
            [schnaq.links :as links]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :as log]))

(>defn- send-invite-emails
  "Expects a list of recipients and the meeting which shall be send."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash recipients]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))
        share-link (links/get-share-link share-hash)]
    (log/debug "Invite Emails for some discussion sent")
    (ok (merge
          {:message "Emails sent successfully"}
          (emails/send-mails
            (format (email-templates :invitation/title) discussion-title)
            (format (email-templates :invitation/body) discussion-title share-link)
            recipients)))))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash recipient edit-hash]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))
        admin-center (links/get-admin-link share-hash edit-hash)]
    (log/debug "Send admin link for discussion " discussion-title " via E-Mail")
    (ok (merge
          {:message "Emails sent successfully"}
          (emails/send-mails
            (format (email-templates :admin-center/title) discussion-title)
            (format (email-templates :admin-center/body) discussion-title admin-center)
            [recipient])))))


;; -----------------------------------------------------------------------------

(def email-routes
  ["/emails" {:swagger {:tags ["emails"]}
              :middleware [:discussion/valid-credentials?]
              :parameters {:body {:share-hash :discussion/share-hash
                                  :edit-hash :discussion/edit-hash}}
              :responses {403 at/response-error-body}}
   ["/send-admin-center-link" {:post send-admin-center-link
                               :description (at/get-doc #'send-admin-center-link)
                               :parameters {:body {:recipient string?}}
                               :responses {200 {:body {:message string?
                                                       :failed-sendings (s/coll-of string?)}}}}]
   ["/send-invites" {:post send-invite-emails
                     :description (at/get-doc #'send-invite-emails)
                     :parameters {:body {:recipients (s/coll-of string?)}}
                     :responses {200 {:body {:message string?
                                             :failed-sendings (s/coll-of string?)}}}}]])