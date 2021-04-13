(ns schnaq.api.user
  (:require [clojure.string :as string]
            [compojure.core :refer [PUT routes wrap-routes context]]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.user :as user-db]
            [schnaq.media :as media]
            [schnaq.s3 :as s3]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user."
  [{:keys [identity]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (ok {:registered-user (user-db/register-new-user identity)}))

(defn- create-UUID-file-name
  "Generates a UUID based on a unique id with a file type suffix."
  [id file-type]
  (str (UUID/nameUUIDFromBytes (.getBytes (str id))) "." file-type))

(defn- change-profile-picture
  "Change the profile picture of a user.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity params]}]
  (let [image-type (get-in params [:image :type])]
    (log/info "User" (:id identity) "trying to set profile picture to:" (get-in params [:image :name]))
    (if (shared-config/allowed-mime-types image-type)
      (if-let [{:keys [input-stream image-type content-type]}
               (media/scale-image-to-height (get-in params [:image :content]) config/profile-picture-height)]
        (let [image-name (create-UUID-file-name (:id identity) image-type)
              absolute-url (s3/upload-stream :user/profile-pictures
                                             input-stream
                                             image-name
                                             {:content-type content-type})]
          (log/info "User" (:id identity) "updated their profile picture")
          (ok {:updated-user (user-db/update-profile-picture-url (:id identity) absolute-url)}))
        (do
          (log/warn "Conversion of image failed for user" (:id identity))
          (bad-request {:error :scaling
                        :message "Could not scale image"})))
      (bad-request {:error :invalid-file-type
                    :message (format "Invalid image uploaded. Received %s, expected one of: %s" image-type (string/join ", " shared-config/allowed-mime-types))}))))

(defn- change-display-name
  "change the display name of a registered user"
  [{:keys [body-params identity]}]
  (let [{:keys [display-name]} body-params]
    (ok {:updated-user (user-db/update-display-name (:id identity) display-name)})))

(def user-routes
  (->
    (routes
      (context "/user" []
        (PUT "/register" [] register-user-if-they-not-exist)
        (PUT "/picture" [] change-profile-picture)
        (PUT "/name" [] change-display-name)))
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))