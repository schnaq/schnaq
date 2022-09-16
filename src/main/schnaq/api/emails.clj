(ns schnaq.api.emails
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs]
            [schnaq.links :as links]
            [schnaq.mail.emails :as emails]
            [schnaq.translations :refer [email-templates]]
            [taoensso.timbre :as log]))

(>defn- send-admin-center-link
  "Send URL to admin-center via mail to recipient."
  [{:keys [parameters]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash recipient]} (:body parameters)
        discussion-title (:discussion/title (discussion-db/discussion-by-share-hash share-hash))
        admin-center (links/get-moderator-center-link share-hash)]
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
              :middleware [:discussion/valid-share-hash?]
              :parameters {:body {:share-hash :discussion/share-hash}}
              :responses {403 at/response-error-body}}
   ["/send-admin-center-link" {:post send-admin-center-link
                               :description (at/get-doc #'send-admin-center-link)
                               :parameters {:body {:recipient string?}}
                               :responses {200 {:body {:message string?
                                                       :failed-sendings (s/coll-of string?)}}}}]])
