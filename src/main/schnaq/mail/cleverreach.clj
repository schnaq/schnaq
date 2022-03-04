(ns schnaq.mail.cleverreach
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- => ?]]
            [muuntaja.core :as m]
            [schnaq.config.cleverreach :as cconfig]
            [schnaq.database.specs :as specs]
            [schnaq.mail.emails :as emails]
            [taoensso.timbre :as log]))

(def ^:private token-url "https://rest.cleverreach.com/oauth/token.php")

(>defn get-access-token
  "Query an access token. Necessary to browse CleverReache's API."
  [client-id client-secret]
  [string? string? => (? map?)]
  (when cconfig/enabled?
    (try
      (let [response
            (m/decode-response-body
             (client/post
              token-url
              {:basic-auth [client-id client-secret]
               :body (m/encode "application/json" {"grant_type" "client_credentials"})
               :content-type :json
               :accept :json}))]
        (log/info "Successfully retrieved access token for CleverReach.")
        response)
      (catch Exception e
        (let [error (ex-data e)]
          (log/error "Could not retrieve access token:" error)
          (emails/send-mail
           "[💥 CleverReach] Konnte keinen access token abrufen"
           (with-out-str (pprint error))
           "christian@schnaq.com")
          error)))))

(def ^:private access-token
  (:access_token (get-access-token cconfig/client-id cconfig/client-secret)))

;; -----------------------------------------------------------------------------

(>defn- wrap-catch-exception
  "Do API call, catch exception and print result or error."
  [email success-log error-log fn]
  [string? ::specs/email string? fn? => (? map?)]
  (if cconfig/enabled?
    (try
      (let [response (fn)]
        (log/debug (format success-log email cconfig/receiver-group))
        response)
      (catch Exception e
        (let [error (ex-data e)]
          (log/error (format "%s mail: %s, body: %s"
                             error-log email (m/decode-response-body error)))
          error)))
    (log/debug "Cleverreach is not enabled.")))

;; -----------------------------------------------------------------------------

(>defn- email->group!
  "Add an email address to a group in Cleverreach, i.e. a list of receivers."
  [email]
  [::specs/email => (? map?)]
  (wrap-catch-exception
   email "Added mail %s to group" "User could not be added to cleverreach."
   #(client/post
     (format "https://rest.cleverreach.com/v3/groups.json/%s/receivers?token=%s" cconfig/receiver-group access-token)
     {:body
      (m/encode "application/json"
                {:email email
                 :registered (quot (System/currentTimeMillis) 1000)
                 :activated 0
                 :source "schnaq Backend"})
      :content-type :json
      :accept :json})))

(>defn- send-double-opt-in!
  "Send double-opt-in mail to user."
  [email]
  [::specs/email => (? map?)]
  (wrap-catch-exception
   email "Double-opt-in mail sent to %s." "Could not trigger opt-in mail."
   #(client/post
     (format "https://rest.cleverreach.com/v3/forms.json/%s/send/activate?token=%s" cconfig/double-opt-in-form access-token)
     {:body
      (m/encode "application/json"
                {:email email
                 :doidata {:user_ip "0.0.0.0"
                           :referer "https://schnaq.com"
                           :user_agent "schnaq/backend"}})
      :content-type :json
      :accept :json})))

(>defn add-pro-tag!
  "Send pro-information of user to cleverreach. Adds a tag to the user's entry."
  [email]
  [::specs/email => (? map?)]
  (wrap-catch-exception
   email "Added pro tag to mail %s." "Could not add pro tag to mail."
   #(client/post
     (format "https://rest.cleverreach.com/v3/receivers.json/%s/tags?token=%s" email access-token)
     {:body (m/encode "application/json" {:tags ["pro"]})
      :content-type :json
      :accept :json})))

(>defn remove-pro-tag!
  "Remove pro tag information from user."
  [email]
  [::specs/email => (? map?)]
  (wrap-catch-exception
   email "Removed pro tag from mail %s." "Could not remove pro tag from mail."
   #(client/delete
     ;; The tags, which should be removed, must be a single tag or a comma separated list of tags.
     (format "https://rest.cleverreach.com/v3/receivers.json/%s/tags/pro?token=%s" email access-token)
     {:content-type :json
      :accept :json})))

(>defn add-new-registered-mail-to-cleverreach
  "Add new mail address to cleverreach."
  [email]
  [::specs/email => (? map?)]
  (email->group! email)
  (send-double-opt-in! email))
