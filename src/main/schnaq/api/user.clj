(ns schnaq.api.user
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [ring.util.http-response :refer [ok bad-request created]]
            [schnaq.api.toolbelt :as at]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.emails :as mail]
            [schnaq.media :as media]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user. New
  users will receive a welcome mail. `creation-secrets` can optionally be provided
  to associate previous created entities with the registered user."
  [{:keys [identity parameters]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (let [{:keys [creation-secrets visited-hashes]} (:body parameters)
        visited-schnaqs (map :db/id (discussion-db/valid-discussions-by-hashes visited-hashes))
        [new-user? queried-user] (user-db/register-new-user identity visited-schnaqs)
        updated-statements? (associative? (discussion-db/update-authors-from-secrets
                                            creation-secrets (:db/id queried-user)))
        response {:registered-user queried-user
                  :updated-statements? updated-statements?}]
    (if new-user?
      (do (mail/send-welcome-mail (:email identity))
          (created "" response))
      (ok response))))

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
          (bad-request (at/build-error-body :scaling "Could not scale image"))))
      (bad-request (at/build-error-body
                     :invalid-file-type
                     (format "Invalid image uploaded. Received %s, expected one of: %s" image-type (string/join ", " shared-config/allowed-mime-types)))))))

(defn- change-display-name
  "Change the display name of a registered user."
  [{:keys [parameters identity]}]
  (let [display-name (get-in parameters [:body :display-name])]
    (ok {:updated-user (user-db/update-display-name (:id identity) display-name)})))


;; -----------------------------------------------------------------------------

(s/def ::creation-secrets map?)
(s/def ::visited-hashes (s/coll-of :discussion/share-hash))
(s/def ::user-register (s/keys :req-un [::visited-hashes]
                               :opt-un [::creation-secrets]))

(def user-routes
  ["/user" {:swagger {:tags ["user"]}
            :middleware [auth/auth-middleware]}
   ["/register" {:put register-user-if-they-not-exist
                 :description (:doc (meta #'register-user-if-they-not-exist))
                 :parameters {:body ::user-register}
                 :responses {201 {:body {:registered-user ::specs/registered-user
                                         :updated-statements? boolean?}}
                             200 {:body {:registered-user ::specs/registered-user
                                         :updated-statements? boolean?}}}}]
   ["/picture" {:put change-profile-picture
                :description (:doc (meta #'change-profile-picture))
                :parameters {:body {:image ::image}}
                :responses {200 {:body {:updated-user ::specs/registered-user}}
                            400 {:body ::at/error-body}}}]
   ["/name" {:put change-display-name
             :description (:doc (meta #'change-display-name))
             :parameters {:body {:display-name :user/nickname}}
             :responses {200 {:body {:updated-user ::specs/registered-user}}}}]])
