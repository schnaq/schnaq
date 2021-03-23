(ns schnaq.api.user
  (:require [compojure.core :refer [PUT routes wrap-routes context]]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.database.user :as user-db]
            [taoensso.timbre :as log]))

(defn- register-user-if-they-not-exist
  "Register a new user if they do not exist. In all cases return the user."
  [{:keys [identity]}]
  (log/info "User-Registration queried for" (:id identity)
            ", username:" (:preferred_username identity))
  (ok {:registered-user (user-db/register-new-user identity)}))

(def user-routes
  (->
    (routes
      (context "/user" []
        (PUT "/register" [] register-user-if-they-not-exist)))
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))