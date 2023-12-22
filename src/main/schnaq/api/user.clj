(ns schnaq.api.user
  (:require [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [ring.util.http-response :refer [bad-request created ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.database.user-deletion :as user-deletion]
            [schnaq.mail.cleverreach :as cleverreach]
            [schnaq.media :as media]
            [schnaq.shared-toolbelt :refer [remove-nil-values-from-map]]
            [spec-tools.data-spec :as ds]
            [taoensso.timbre :as log]))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New
  users will receive a welcome mail. `creation-secrets` can optionally be provided
  to associate previous created entities with the registered user."
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes visited-statement-ids locale]} (:body parameters)
        visited-schnaqs (if visited-hashes (map :db/id (discussion-db/discussions-by-share-hashes visited-hashes)) [])
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs visited-statement-ids)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                           creation-secrets (:db/id queried-user)))
        response {:registered-user queried-user
                  :updated-statements? updated-statements?
                  :meta (remove-nil-values-from-map
                         {:total-schnaqs (user-db/created-discussions (:user.registered/keycloak-id queried-user))})}]
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
        keycloak-id (:id identity)]
    (log/info (format "User %s trying to set profile picture to %s" keycloak-id (:name image)))
    (let [file-name (path-to-file keycloak-id (:type image))
          {:keys [url error message]} (media/upload-image! image file-name config/profile-picture-width :user/media)]
      (if url
        (ok {:updated-user (user-db/update-user {:user.registered/keycloak-id keycloak-id
                                                 :user.registered/profile-picture url})})
        (bad-request (at/build-error-body error message))))))

(defn- change-display-name
  "Change the display name of a registered user."
  [{:keys [parameters user]}]
  (let [display-name (get-in parameters [:body :display-name])]
    (ok {:updated-user
         (user-db/update-user (assoc (select-keys user [:user.registered/keycloak-id])
                                     :user.registered/display-name display-name))})))

(defn- change-notification-mail-interval
  "Change the interval a user receives notification mails"
  [{:keys [parameters user]}]
  (let [interval (get-in parameters [:body :notification-mail-interval])]
    (ok {:updated-user (user-db/update-user (assoc (select-keys user [:user.registered/keycloak-id])
                                                   :user.registered/notification-mail-interval interval))})))

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

(defn- get-user
  "Return a user with all personal information."
  [{{{:keys [keycloak-id]} :query} :parameters}]
  (ok {:user (user-db/private-user-by-keycloak-id keycloak-id)}))

(defn- update-user
  "Update a field of a user."
  [{{{:keys [user]} :body} :parameters}]
  (ok {:user (user-db/update-user user)}))

(defn- delete-user-field
  "Delete a field from the user."
  [{{{:keys [keycloak-id attribute value]} :body} :parameters}]
  (if value
    (ok {:user (user-db/retract-user-attributes-value {:user.registered/keycloak-id keycloak-id} attribute value)})
    (ok {:user (user-db/retract-user-attribute {:user.registered/keycloak-id keycloak-id} attribute)})))

;; -----------------------------------------------------------------------------

(s/def ::creation-secrets map?)
(s/def ::visited-hashes (s/coll-of :discussion/share-hash))
(s/def ::visited-statement-ids map?)
(s/def ::locale keyword?)
(s/def ::user-register (s/keys :opt-un [::visited-hashes
                                        ::creation-secrets
                                        ::visited-statement-ids
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
                                           :updated-statements? boolean?}}
                               200 {:body {:registered-user ::specs/registered-user
                                           :updated-statements? boolean?}}}}]
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
    ["" {:get {:handler get-user
               :description (at/get-doc #'get-user)
               :parameters {:query {:keycloak-id :user.registered/keycloak-id}}}
         :put {:handler update-user
               :description (at/get-doc #'update-user)
               :parameters {:body {:user ::specs/registered-user}}
               :responses {200 {:body {:user ::specs/registered-user}}}}
         :delete {:handler delete-user-field
                  :description (at/get-doc #'delete-user-field)
                  :parameters {:body {:keycloak-id :user.registered/keycloak-id
                                      :attribute keyword?
                                      (ds/opt :value) any?}}
                  :responses {200 {:body {:user ::specs/registered-user}}}}
         :name :api.admin/user
         :responses {200 {:body {:user ::specs/registered-user}}}}]
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
