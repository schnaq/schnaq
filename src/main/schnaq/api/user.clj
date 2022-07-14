(ns schnaq.api.user
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [ring.util.http-response :refer [bad-request created ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.database.user-deletion :as user-deletion]
            [schnaq.mail.cleverreach :as cleverreach]
            [schnaq.media :as media]
            [taoensso.timbre :as log]))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New
  users will receive a welcome mail. `creation-secrets` can optionally be provided
  to associate previous created entities with the registered user. Same goes for `schnaq-creation-secrets`"
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes visited-statement-ids schnaq-creation-secrets locale]} (:body parameters)
        visited-schnaqs (if visited-hashes (map :db/id (discussion-db/discussions-by-share-hashes visited-hashes)) [])
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs visited-statement-ids)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                           creation-secrets (:db/id queried-user)))
        updated-schnaqs? (associative? (discussion-db/update-schnaq-authors schnaq-creation-secrets (:db/id queried-user)))
        response {:registered-user queried-user
                  :updated-statements? updated-statements?
                  :updated-schnaqs? updated-schnaqs?}]
    (if new-user?
      (do (cleverreach/add-user-to-customer-group! identity (str (name locale)))
          (created "" (assoc response :new-user? true)))
      (ok response))))

;; -----------------------------------------------------------------------------

(>defn- path-to-file
  "Store the profile picture in the user's media directory."
  [user-id file-type]
  [:user.registered/keycloak-id :file/type => string?]
  (format "%s/images/profile.%s" user-id
          (media/image-type->file-ending file-type)))

(defn- change-profile-picture
  "Change the profile picture of a user.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity parameters]}]
  (let [image (get-in parameters [:body :image])
        user-id (:id identity)]
    (log/info (format "User %s trying to set profile picture to %s" user-id (:name image)))
    (let [file-name (path-to-file user-id (:type image))
          {:keys [url error message]} (media/upload-image! image file-name config/profile-picture-width :user/media)]
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

(defn- add-role
  "Add a role to a user."
  [{{{:keys [keycloak-id role]} :body} :parameters}]
  (let [roles (:user.registered/roles (user-db/add-role keycloak-id role))]
    (ok {:roles roles})))

(defn- remove-role
  "Remove a role from a user."
  [{{{:keys [keycloak-id role]} :body} :parameters}]
  (let [roles (:user.registered/roles (user-db/remove-role keycloak-id role))]
    (ok {:roles roles})))

;; -----------------------------------------------------------------------------

(s/def ::creation-secrets map?)
(s/def ::visited-hashes (s/coll-of :discussion/share-hash))
(s/def ::visited-statement-ids map?)
(s/def ::schnaq-creation-secrets map?)
(s/def ::locale keyword?)
(s/def ::user-register (s/keys :opt-un [::visited-hashes
                                        ::creation-secrets
                                        ::visited-statement-ids
                                        ::schnaq-creation-secrets
                                        ::locale]))

(def user-routes
  [["/user" {:swagger {:tags ["user"]}}
    ["/anonymous/add" {:put add-anonymous-user
                       :description (at/get-doc #'add-anonymous-user)
                       :parameters {:body {:nickname :user/nickname}}
                       :responses {201 {:body {:user-id :db/id}}}}]
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
                  :responses {200 {:body {:deleted? boolean?}}}}]
    ["/role" {:put {:handler add-role
                    :description (at/get-doc #'add-role)}
              :delete {:handler remove-role
                       :description (at/get-doc #'remove-role)}
              :name :api.admin.user/role
              :parameters {:body {:keycloak-id :user.registered/keycloak-id
                                  :role :user.registered/valid-roles}}
              :responses {200 {:body {:roles :user.registered/roles}}}}]]])
