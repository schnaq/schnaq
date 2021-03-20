(ns schnaq.api.hubs
  (:require [compojure.core :refer [GET routes context]]
            [ring.util.http-response :refer [ok]]
            [schnaq.database.hub :as hub-db]))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [request]
  (let [keycloak-name (get-in request [:route-params :keycloak-name])]
    (ok {:hubs (hub-db/hub-by-keycloak-name keycloak-name)})))

(def hub-routes
  (->
    (routes
      (context "/hubs" []
        (GET "/:keycloak-name" [] hub-by-keycloak-name)))))