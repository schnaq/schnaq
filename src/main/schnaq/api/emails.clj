(ns schnaq.api.emails
  (:require [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.emails :as emails]
            [schnaq.translations :refer [email-templates]]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(>defn- send-invite-emails
  "Expects a list of recipients and the meeting which shall be send."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipients share-link]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug "Invite Emails for some meeting sent")
          (ok (merge
                {:message "Emails sent successfully"}
                (emails/send-mails
                  (format (email-templates :invitation/title) discussion-title)
                  (format (email-templates :invitation/body) discussion-title share-link)
                  recipients))))
      (validator/deny-access))))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash recipient admin-center]} (:body parameters)
        meeting-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug "Send admin link for meeting " meeting-title " via E-Mail")
          (ok (merge
                {:message "Emails sent successfully"}
                (emails/send-mails
                  (format (email-templates :admin-center/title) meeting-title)
                  (format (email-templates :admin-center/body) meeting-title admin-center)
                  [recipient]))))
      (validator/deny-access))))


;; -----------------------------------------------------------------------------

(def email-routes
  ["/emails" {:swagger {:tags ["emails"]}
              :parameters {:body {:share-hash :discussion/share-hash
                                  :edit-hash :discussion/edit-hash}}}
   ["/send-admin-center-link" {:post send-admin-center-link
                               :description (at/get-doc #'send-admin-center-link)
                               :parameters {:body {:recipient string?
                                                   :admin-center string?}}
                               :responses {200 {:body {:message string?
                                                       :failed-sendings (s/coll-of string?)}}
                                           403 at/response-error-body}}]
   ["/send-invites" {:post send-invite-emails
                     :description (at/get-doc #'send-invite-emails)
                     :parameters {:body {:recipients (s/coll-of string?)
                                         :share-link :discussion/share-link}}
                     :responses {200 {:body {:message string?
                                             :failed-sendings (s/coll-of string?)}}
                                 403 at/response-error-body}}]])