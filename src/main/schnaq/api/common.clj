(ns schnaq.api.common
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http-client]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config.mailchimp :as mailchimp-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.emails :as emails]
            [schnaq.export :as export]
            [taoensso.timbre :as log]))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- check-credentials!
  "Checks whether share-hash and edit-hash match.
  If the user is logged in and the credentials are valid, they are added as an admin."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (when keycloak-id
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (ok {:valid-credentials? true})))

(defn- export-txt-data
  "Exports the discussion data as a string."
  [{:keys [parameters]}]
  (let [{:keys [share-hash]} (:query parameters)]
    (log/info "User is generating a txt export for discussion" share-hash)
    (ok {:string-representation (export/generate-text-export share-hash)})))

(defn- subscribe-lead-magnet!
  "Subscribes to the mailing list and sends the lead magnet to the email-address."
  [{:keys [parameters]}]
  (let [email (get-in parameters [:body :email])
        options {:timeout 10000
                 :basic-auth ["user" mailchimp-config/api-key]
                 :body (json/write-str {:email_address email
                                        :status "subscribed"
                                        :email_type "html"
                                        :tags ["lead-magnet" "datenschutz"]})
                 :user-agent "schnaq Backend Application"}]
    (http-client/post mailchimp-config/subscribe-uri options)
    (if (emails/send-remote-work-lead-magnet email)
      (ok {:status :ok})
      (bad-request (at/build-error-body :failed-subscription "Something went wrong. Check your Email-Address and try again.")))))


;; -----------------------------------------------------------------------------

(def other-routes
  [["" {:swagger {:tags ["other"]}}
    ["/ping" {:get ping
              :description (at/get-doc #'ping)
              :responses {200 {:body {:text string?}}}}]
    ["/export/txt" {:get export-txt-data
                    :description (at/get-doc #'export-txt-data)
                    :middleware [:discussion/valid-share-hash?]
                    :parameters {:query {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:string-representation string?}}
                                404 at/response-error-body}}]
    ["/credentials/validate" {:post check-credentials!
                              :description (at/get-doc #'check-credentials!)
                              :middleware [:discussion/valid-credentials?]
                              :responses {200 {:body {:valid-credentials? boolean?}}
                                          403 at/response-error-body}
                              :parameters {:body {:share-hash :discussion/share-hash
                                                  :edit-hash :discussion/edit-hash}}}]
    ["/lead-magnet/subscribe" {:post subscribe-lead-magnet!
                               :description (at/get-doc #'subscribe-lead-magnet!)
                               :parameters {:body {:email string?}}
                               :responses {200 {:body {:status keyword?}}
                                           400 at/response-error-body}}]]])