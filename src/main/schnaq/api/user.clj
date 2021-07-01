(ns schnaq.api.user
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as mail]
            [schnaq.media :as media]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New users will receive a welcome
  mail."
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes]} (:body parameters)
        visited-schnaqs (map :db/id (discussion-db/valid-discussions-by-hashes visited-hashes))
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                            creation-secrets (:db/id queried-user)))]
    (when new-user?
      (mail/send-welcome-mail (:email identity)))
    (ok {:registered-user queried-user
         :updated-statements? updated-statements?})))

(defn- create-UUID-file-name
  "Generates a UUID based on a unique id with a file type suffix."
  [id file-type]
  (str (UUID/nameUUIDFromBytes (.getBytes (str id))) "." file-type))

(s/def :image/type string?)
(s/def :image/name string?)
(s/def :image/content string?)
(s/def ::image
  (s/keys :req-un [:image/type :image/name :image/content]))

(defn- change-profile-picture
  "Change the profile picture of a user.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity parameters]}]
  (let [image-type (get-in parameters [:body :image :type])
        image-name (get-in parameters [:body :image :name])
        image-content (get-in parameters [:body :image :content])]
    (log/info "User" (:id identity) "trying to set profile picture to:" image-name)
    (if (shared-config/allowed-mime-types image-type)
      (if-let [{:keys [input-stream image-type content-type]}
               (media/scale-image-to-height image-content config/profile-picture-height)]
        (let [image-name (create-UUID-file-name (:id identity) image-type)
              absolute-url (s3/upload-stream :user/profile-pictures
                                             input-stream
                                             image-name
                                             {:content-type content-type})]
          (log/info "User" (:id identity) "updated their profile picture to" absolute-url)
          (ok {:updated-user (user-db/update-profile-picture-url (:id identity) absolute-url)}))
        (do
          (log/warn "Conversion of image failed for user" (:id identity))
          (bad-request {:error :scaling
                        :message "Could not scale image"})))
      (bad-request {:error :invalid-file-type
                    :message (format "Invalid image uploaded. Received %s, expected one of: %s" image-type (string/join ", " shared-config/allowed-mime-types))}))))

(defn- change-display-name
  "Change the display name of a registered user"
  [{:keys [parameters identity]}]
  (let [display-name (get-in parameters [:body :display-name])]
    (ok {:updated-user (user-db/update-display-name (:id identity) display-name)})))


;; -----------------------------------------------------------------------------

(def user-routes
  ["/user" {:swagger {:tags ["user"]}
            :middlewares [auth/auth-middleware]}
   ["/register" {:put register-user-if-they-not-exist
                 :parameters {:body {:creation-secrets (s/? map?)
                                     :visited-hashes (s/coll-of string?)}}}]
   ["/picture" {:put change-profile-picture
                :parameters {:body {:image ::image}}}]
   ["/name" {:put change-display-name
             :parameters {:body {:display-name string?}}}]])
