(ns schnaq.api.user
  (:require [compojure.core :refer [PUT routes wrap-routes context]]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.config :as config]
            [schnaq.database.user :as user-db]
            [schnaq.media :as media]
            [taoensso.timbre :as log]))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user."
  [{:keys [identity]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (ok {:registered-user (user-db/register-new-user identity)}))

(defn- change-profile-picture [{:keys [params]}]
  (let [image (:image params)
        scaled-image-stream (media/scale-image-to-height (:content image) config/profile-picture-height)]
    (if scaled-image-stream
      (ok "Neues Profil Bild angelegt"))))

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