(ns schnaq.api.hubs
  (:require [compojure.core :refer [GET routes wrap-routes context]]
            [ring.util.http-response :refer [ok forbidden]]
            [schnaq.auth :as auth]
            [schnaq.database.hub :as hub-db]))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [request]
  (let [keycloak-name (get-in request [:params :keycloak-name])]
    (if (auth/group-membership? request keycloak-name)
      (ok {:hub (hub-db/hub-by-keycloak-name keycloak-name)})
      (forbidden "You are not allowed to access this ressource."))))

(def hub-routes
  (->
    (routes
      (context "/hub" []
        (GET "/:keycloak-name" [] hub-by-keycloak-name)))
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))