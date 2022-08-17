(ns schnaq.mail.cleverreach
  (:require [clj-http.client :as client]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- => ?]]
            [muuntaja.core :as m]
            [schnaq.config.cleverreach :as cconfig]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log]))

(def ^:private token-url "https://rest.cleverreach.com/oauth/token.php")

(>defn get-access-token
  "Query an access token. Necessary to browse CleverReach's API."
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
          (toolbelt/post-error-in-chat "CleverReach" (format "Could not retrieve access token: `%s`" error))
          error)))))

(defonce ^:private access-token
  (atom (:access_token (get-access-token cconfig/client-id cconfig/client-secret))))

;; -----------------------------------------------------------------------------

(>defn- retry-with-new-token
  "Retry the last operation with a freshly queried token. If it does not work again, give up."
  [fn]
  [fn? => (? map?)]
  (reset! access-token (:access_token (get-access-token cconfig/client-id cconfig/client-secret)))
  (try
    (fn)
    (toolbelt/post-in-mattermost! "[CleverReach] Recovered last operation with fresh token")
    (catch Exception e
      (let [error (ex-data e)]
        (toolbelt/post-error-in-chat "CleverReach"
                                     (format "Could not recover with new token, Giving up. Error %s" error))
        error))))

(>defn- wrap-catch-exception
  "Do API call, catch exception and print result or error."
  [email success-log error-log fn]
  [::specs/email string? string? fn? => (? map?)]
  (if cconfig/enabled?
    (try
      (let [response (fn)]
        (log/debug (format success-log email cconfig/receiver-group))
        response)
      (catch Exception e
        (let [error (ex-data e)
              response-body (m/decode-response-body error)
              formatted-error (format "%s mail: %s, body: `%s`" error-log email response-body)]
          (toolbelt/post-error-in-chat "CleverReach" formatted-error)
          (log/error formatted-error)
          (if (= "Unauthorized: token expired" (:message response-body))
            (retry-with-new-token fn)
            error))))
    (log/debug "Cleverreach is not enabled.")))

;; -----------------------------------------------------------------------------

(>defn add-user-to-customer-group!
  "Add an email address to a group in Cleverreach, i.e. a list of receivers."
  [{:keys [email sub given_name family_name]} locale]
  [::specs/identity ::specs/non-blank-string => (? map?)]
  (wrap-catch-exception
   email "Added mail %s to group" "User could not be added to cleverreach."
   #(client/post
     (format "https://rest.cleverreach.com/v3/groups.json/%s/receivers?token=%s" cconfig/receiver-group @access-token)
     {:body
      (m/encode "application/json"
                {:email email
                 :tags ["customer-free"]
                 :source "schnaq Backend"
                 :global_attributes (cond-> {:locale locale
                                             :keycloak_id sub}
                                      given_name (assoc :firstname given_name)
                                      family_name (assoc :lastname family_name))})
      :content-type :json
      :accept :json})))

(>defn add-tag!
  "Adds a tag to the user's entry in cleverreach."
  [email tags]
  [::specs/email (s/and vector? (s/coll-of string?)) => (? map?)]
  (wrap-catch-exception
   email "Added tag to mail %s." "Could not add tag to mail."
   #(client/post
     (format "https://rest.cleverreach.com/v3/receivers.json/%s/tags?token=%s" email @access-token)
     {:body (m/encode "application/json" {:tags tags
                                          :group_id cconfig/receiver-group})
      :content-type :json
      :accept :json})))

(>defn add-pro-tag!
  "Adds a pro tag to the user's entry."
  [email]
  [::specs/email => (? map?)]
  (add-tag! email ["customer-pro"]))

(>defn add-free-tag!
  "Adds free tag to the user's entry."
  [email]
  [::specs/email => (? map?)]
  (add-tag! email ["customer-free"]))

(>defn remove-tag!
  "Remove one tag information from user."
  [email tag]
  [::specs/email string? => (? map?)]
  (wrap-catch-exception
   email "Removed tag from mail %s." "Could not remove tag from mail."
   #(client/delete
     ;; The tags, which should be removed, must be a single tag or a comma separated list of tags.
     (format "https://rest.cleverreach.com/v3/receivers.json/%s/tags/%s?token=%s" email tag @access-token)
     {:content-type :json
      :accept :json})))

(>defn remove-pro-tag!
  "Remove pro tag information from user."
  [email]
  [::specs/email => (? map?)]
  (remove-tag! email "customer-pro"))

(>defn remove-free-tag!
  "Remove free tag information from user."
  [email]
  [::specs/email => (? map?)]
  (remove-tag! email "customer-free"))
