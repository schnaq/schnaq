(ns schnaq.api.hub
  (:require [compojure.core :refer [GET routes wrap-routes context]]
            [ring.util.http-response :refer [ok forbidden]]
            [schnaq.auth :as auth]
            [schnaq.database.hub :as hub-db]
            [schnaq.processors :as processors]))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [request]
  (let [keycloak-name (get-in request [:params :keycloak-name])]
    (if (auth/member-of-group? request keycloak-name)
      (let [hub (hub-db/hub-by-keycloak-name keycloak-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      (forbidden "You are not allowed to access this ressource."))))

(defn- all-hubs-for-user
  "Return all valid hubs for a user."
  [request]
  (let [keycloak-names (get-in request [:identity :groups])]
    (ok {:hubs (hub-db/hubs-by-keycloak-names keycloak-names)})))

(def hub-routes
  (->
    (routes
      (context "/hubs" []
        (GET "/personal" [] all-hubs-for-user))
      (context "/hub" []
        (GET "/:keycloak-name" [] hub-by-keycloak-name)))
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))