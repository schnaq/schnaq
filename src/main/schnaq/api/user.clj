(ns schnaq.api.user
  (:require [clj-http.client :as client]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [keycloak.admin :as kc-admin]
            [muuntaja.core :as m]
            [ring.util.http-response :refer [bad-request created ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.config.keycloak :as kc-config :refer [kc-client]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.database.user-deletion :as user-deletion]
            [schnaq.mail.cleverreach :as cleverreach]
            [schnaq.media :as media]
            [taoensso.timbre :as log]))

(s/def ::access_token (s/and ::specs/non-blank-string #(.startsWith % "ey"))) ;; eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2..
(s/def ::refresh_token (s/and ::specs/non-blank-string #(.startsWith % "ey"))) ;; eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2...
(s/def ::token_type ::specs/non-blank-string) ;; Bearer
(s/def ::scope ::specs/non-blank-string) ;; "email profile"
(s/def ::tokens
  (s/keys :opt-un [::access_token ::refresh_token ::token_type ::scope]))
(s/def :login-error/error string?)
(s/def :login-error/error_description string?)
(s/def ::login-error
  (s/keys :req-un [:login-error/error :login-error/error_description]))

(s/def :registration-response/new? boolean?)
(s/def ::registration-response
  (s/keys :req-un [:registration-response/new? :user.registered/email]
          :opt-un [::tokens]))

(>defn- login-user-at-keycloak
  "Login a user at keycloak. Return the tokens if login was successful."
  [email password]
  [:user.registered/email string? => (s/or :tokens ::tokens :error ::login-error)]
  (try
    (let [response
          (:body
           (client/post (format "%s/token" kc-config/openid-endpoint)
                        {:headers {:content-type "application/x-www-form-urlencoded"}
                         :form-params {:grant_type "password"
                                       :client_id kc-config/client-id
                                       :username email
                                       :password password}
                         :as :json}))]
      response)
    (catch Exception e
      (let [body (m/decode-response-body (ex-data e))]
        (log/info "Could not retrieve token for user:" body)
        body))))

(defn- user-registration
  "Register new user if she does not already exist. On new registration, returns
  the tokens to the user."
  [{{{:keys [email password]} :body} :parameters}]
  (if (user-db/user-by-email email)
    (ok {:new? false
         :email email})
    (let [_ (kc-admin/create-user! kc-client kc-config/realm {:email email :password password})
          tokens (login-user-at-keycloak email password)]
      (log/debug "Registered new user:" email)
      (if (:access_token tokens)
        (ok {:new? true :tokens tokens :email email})
        (bad-request (at/build-error-body (keyword (:error tokens)) (:error_description tokens)))))))

;; -----------------------------------------------------------------------------

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New
  users will receive a welcome mail. `creation-secrets` can optionally be provided
  to associate previous created entities with the registered user. Same goes for `schnaq-creation-secrets`"
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes visited-statement-ids schnaq-creation-secrets]} (:body parameters)
        visited-schnaqs (if visited-hashes (map :db/id (discussion-db/discussions-by-share-hashes visited-hashes)) [])
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs visited-statement-ids)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                           creation-secrets (:db/id queried-user)))
        updated-schnaqs? (associative? (discussion-db/update-schnaq-authors schnaq-creation-secrets (:db/id queried-user)))
        response {:registered-user queried-user
                  :updated-statements? updated-statements?
                  :updated-schnaqs? updated-schnaqs?}]
    (if new-user?
      (do (cleverreach/add-user-to-customer-group! identity)
          (created "" (assoc response :new-user? true)))
      (ok response))))

;; -----------------------------------------------------------------------------

(>defn- path-to-file
  "Store the profile picture in the user's media directory."
  [user-id file-type]
  [:user.registered/keycloak-id :file/type => string?]
  (format "%s/images/profile.%s" user-id
          (media/mime-type->file-ending file-type)))

(defn- change-profile-picture
  "Change the profile picture of a user.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity parameters]}]
  (let [image (get-in parameters [:body :image])
        user-id (:id identity)]
    (log/info (format "User %s trying to set profile picture to %s" user-id (:name image)))
    (let [file-name (path-to-file user-id (:type image))
          {:keys [url error message]}
          (media/upload-image! file-name (:type image) (:content image) config/profile-picture-width :user/media)]
      (if url
        (ok {:updated-user (user-db/update-profile-picture-url user-id url)})
        (bad-request (at/build-error-body error message))))))

(defn- change-display-name
  "Change the display name of a registered user."
  [{:keys [parameters identity]}]
  (let [display-name (get-in parameters [:body :display-name])]
    (ok {:updated-user (user-db/update-display-name (:id identity) display-name)})))

(defn- change-notification-mail-interval
  "Change the interval a user receives notification mails"
  [{:keys [parameters identity]}]
  (let [interval (get-in parameters [:body :notification-mail-interval])
        user (user-db/update-notification-mail-interval (:id identity) interval)]
    (ok {:updated-user user})))

(defn- mark-all-statements-as-read
  "Mark all statements of a user's visited schnaqs as read"
  [{:keys [identity]}]
  (ok {:new-statements (discussion-db/mark-all-statements-as-read! (:id identity))}))

(defn- add-anonymous-user
  "Generate a user based on the nickname. This is an *anonymous* user, and we
  can only refer to the user by the nickname. So this function is idempotent and
  returns always the same id when providing the same nickname."
  [{:keys [parameters]}]
  (let [author-name (get-in parameters [:body :nickname])
        user-id (user-db/add-user-if-not-exists author-name)]
    (created "" {:user-id user-id})))

;; -----------------------------------------------------------------------------
;; Deletion

(defn- delete-all-statements-for-user
  "Delete all statements from a given user."
  [{{{:keys [keycloak-id]} :body} :parameters}]
  (user-deletion/delete-all-statements-for-user keycloak-id)
  (ok {:deleted? true}))

(defn- delete-all-discussions-for-user
  "Deletes all discussions where the user is the author."
  [{{{:keys [keycloak-id]} :body} :parameters}]
  (user-deletion/delete-discussions-for-user keycloak-id)
  (ok {:deleted? true}))

(defn- delete-user-identity
  "Deletes a user's personal identity in our system."
  [{{{:keys [keycloak-id]} :body} :parameters}]
  (user-deletion/delete-user-identity keycloak-id)
  (ok {:deleted? true}))

;; -----------------------------------------------------------------------------

(s/def ::creation-secrets map?)
(s/def ::visited-hashes (s/coll-of :discussion/share-hash))
(s/def ::visited-statement-ids map?)
(s/def ::schnaq-creation-secrets map?)
(s/def ::user-register (s/keys :opt-un [::visited-hashes
                                        ::creation-secrets
                                        ::visited-statement-ids
                                        ::schnaq-creation-secrets]))

(def user-routes
  [["/user" {:swagger {:tags ["user"]}}
    ["/anonymous/add" {:put add-anonymous-user
                       :description (at/get-doc #'add-anonymous-user)
                       :parameters {:body {:nickname :user/nickname}}
                       :responses {201 {:body {:user-id :db/id}}}}]
    ["/registration/new" {:post user-registration
                          :description (at/get-doc #'user-registration)
                          :parameters {:body {:email :user.registered/email
                                              :password string?}}
                          :responses {200 {:body ::registration-response}
                                      400 at/response-error-body}}]
    ["" {:middleware [:user/authenticated?]}
     ["/register" {:put register-user-if-they-not-exist
                   :description (at/get-doc #'register-user-if-they-not-exist)
                   :parameters {:body ::user-register}
                   :responses {201 {:body {:registered-user ::specs/registered-user
                                           :updated-statements? boolean?
                                           :updated-schnaqs? boolean?}}
                               200 {:body {:registered-user ::specs/registered-user
                                           :updated-statements? boolean?
                                           :updated-schnaqs? boolean?}}}}]
     ["/picture" {:put change-profile-picture
                  :description (at/get-doc #'change-profile-picture)
                  :parameters {:body {:image ::specs/image}}
                  :responses {200 {:body {:updated-user ::specs/registered-user}}
                              400 at/response-error-body}}]
     ["/name" {:put change-display-name
               :description (at/get-doc #'change-display-name)
               :parameters {:body {:display-name :user/nickname}}
               :responses {200 {:body {:updated-user ::specs/registered-user}}}}]
     ["/notification-mail-interval" {:put change-notification-mail-interval
                                     :description (at/get-doc #'change-notification-mail-interval)
                                     :parameters {:body {:notification-mail-interval keyword?}}
                                     :responses {200 {:body {:updated-user ::specs/registered-user}}
                                                 400 at/response-error-body}}]
     ["/mark-all-as-read" {:put mark-all-statements-as-read
                           :description (at/get-doc #'mark-all-statements-as-read)
                           :parameters {}
                           :responses {200 {:body {:new-statements coll?}}
                                       400 at/response-error-body}}]]]
   ["/admin/user" {:swagger {:tags ["admin"]}
                   :middleware [:user/authenticated? :user/admin?]
                   :responses {400 at/response-error-body}}
    ["/statements" {:delete delete-all-statements-for-user
                    :description (at/get-doc #'delete-all-statements-for-user)
                    :parameters {:body {:keycloak-id :user.registered/keycloak-id}}
                    :responses {200 {:body {:deleted? boolean?}}}}]
    ["/schnaqs" {:delete delete-all-discussions-for-user
                 :description (at/get-doc #'delete-all-discussions-for-user)
                 :parameters {:body {:keycloak-id :user.registered/keycloak-id}}
                 :responses {200 {:body {:deleted? boolean?}}}}]
    ["/identity" {:delete delete-user-identity
                  :description (at/get-doc #'delete-user-identity)
                  :parameters {:body {:keycloak-id :user.registered/keycloak-id}}
                  :responses {200 {:body {:deleted? boolean?}}}}]]])
