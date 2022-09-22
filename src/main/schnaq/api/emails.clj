(ns schnaq.api.emails
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs]
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

;; -----------------------------------------------------------------------------

(def email-routes
  ["/moderation" {:swagger {:tags ["moderation"]}
                  :middleware [:discussion/user-moderator?]
                  :parameters {:body {:share-hash :discussion/share-hash}}
                  :responses {403 at/response-error-body}}
   ["/promote-user" {:name :api.moderation/promote-user
                     :post promote-user-to-moderator
                     :description (at/get-doc #'promote-user-to-moderator)
                     :parameters {:body {:recipient string?
                                         :admin-center string?}}
                     :responses {200 {:body {:message string?
                                             :failed-sendings (s/coll-of string?)}}}}]])
