(ns schnaq.mail.cleverreach
  (:require [clj-http.client :as client]
            [clojure.pprint :refer [pprint]]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
            [muuntaja.core :as m]
            [schnaq.config.cleverreach :as cconfig]
            [schnaq.database.specs :as specs]
            [schnaq.mail.emails :as emails]
            [taoensso.timbre :as log]))

(def ^:private token-url "https://rest.cleverreach.com/oauth/token.php")

(>defn get-access-token
  "Query an access token. Necessary to browse CleverReache's API."
  [client-id client-secret]
  [string? string? => map?]
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
        error))))

(def ^:private access-token
  (:access_token (get-access-token cconfig/client-id cconfig/client-secret)))

(defn- routes
  "Routes to the cleverreach api."
  [route]
  (get
   {:group/add (format "https://rest.cleverreach.com/v3/groups.json/%s/receivers?token=%s" cconfig/receiver-group access-token)
    :double-opt-in/send (format "https://rest.cleverreach.com/v3/forms.json/%s/send/activate?token=%s" cconfig/double-opt-in-form access-token)}
   route))

;; -----------------------------------------------------------------------------

(>defn- email->group!
  "Add an email address to a group in Cleverreach, i.e. a list of receivers."
  [email]
  [::specs/email => map?]
  (try
    (let [response
          (client/post
           (routes :group/add)
           {:body
            (m/encode "application/json"
                      {:email email
                       :registered (quot (System/currentTimeMillis) 1000)
                       :activated 0
                       :source "schnaq Backend"})
            :content-type :json
            :accept :json})]
      (log/debug (format "Added mail %s to group %d" email cconfig/receiver-group))
      response)
    (catch Exception e
      (let [error (ex-data e)]
        (log/error (format "User could not be added to cleverreach. mail: %s, body: %s"
                           email (m/decode-response-body error)))
        error))))

(>defn- send-double-opt-in!
  "Send double-opt-in mail to user."
  [email]
  [::specs/email => map?]
  (try
    (client/post
     (routes :double-opt-in/send)
     {:body
      (m/encode "application/json"
                {:email email
                 :doidata {:user_ip "0.0.0.0"
                           :referer "https://schnaq.com"
                           :user_agent "schnaq/backend"}})
      :content-type :json
      :accept :json})
    (catch Exception e
      (let [error (ex-data e)]
        (log/error (format "Could not trigger opt-in mail. mail: %s, body: %s"
                           email (m/decode-response-body error)))
        error))))

;; -----------------------------------------------------------------------------

(>defn add-new-registered-mail-to-cleverreach
  "Add new mail address to cleverreach."
  [email]
  [::specs/email => map?]
  (if cconfig/enabled?
    (do
      (email->group! email)
      (send-double-opt-in! email))
    (log/info "Cleverreach is not enabled.")))
