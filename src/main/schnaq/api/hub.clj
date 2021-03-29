(ns schnaq.api.hub
  (:require [compojure.core :refer [GET POST DELETE routes wrap-routes context]]
            [ring.util.http-response :refer [ok forbidden bad-request]]
            [schnaq.auth :as auth]
            [schnaq.meeting.database :refer [fast-pull]]
            [schnaq.database.hub :as hub-db]
            [schnaq.processors :as processors]
            [schnaq.validator :as validators]))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [request]
  (let [keycloak-name (get-in request [:params :keycloak-name])]
    (if (auth/member-of-group? (:identity request) keycloak-name)
      (let [hub (hub-db/hub-by-keycloak-name keycloak-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      (forbidden "You are not allowed to access this ressource."))))

(defn- all-hubs-for-user
  "Return all valid hubs for a user."
  [request]
  (let [keycloak-names (get-in request [:identity :groups])
        hubs (hub-db/hubs-by-keycloak-names keycloak-names)
        processed-hubs (map
                         #(update % :hub/schnaqs
                                  (fn [hub] (map processors/add-meta-info-to-schnaq hub)))
                         hubs)]
    (ok {:hubs processed-hubs})))

(defn- add-schnaq-to-hub
  "Adds a schnaq to a hub identified by the group-name. Only allow the adding when
  the schnaq is not exclusively tied to another hub. Also check for appropriate group membership."
  [{:keys [params identity]}]
  (let [{:keys [keycloak-name share-hash]} params]
    (if (auth/member-of-group? identity keycloak-name)
      (if (validators/valid-discussion? share-hash)
        ;; NOTE: When hub-exclusive schnaqs are in, check it in the if above.
        (let [discussion-id (:db/id (fast-pull [:discussion/share-hash share-hash] [:db/id]))
              hub-id (:db/id (fast-pull [:hub/keycloak-name keycloak-name] [:db/id]))
              hub (hub-db/add-discussions-to-hub hub-id [discussion-id])
              processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
          (ok {:hub processed-hub}))
        (bad-request {:message "The discussion could not be found."}))
      (forbidden {:message "You are not a member of the group."}))))

(defn- remove-schnaq-from-hub
  "Removes a schnaq from the specified hub. Only happens when the caller is member of the hub."
  [{:keys [params identity]}]
  (let [{:keys [keycloak-name share-hash]} params]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/remove-discussion-from-hub [:hub/keycloak-name keycloak-name]
                                                   [:discussion/share-hash share-hash])
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      (forbidden {:message "You are not a member of the group."}))))

(def hub-routes
  (->
    (routes
      (context "/hubs" []
        (GET "/personal" [] all-hubs-for-user))
      (context "/hub" []
        (GET "/:keycloak-name" [] hub-by-keycloak-name)
        (POST "/:keycloak-name/add" [] add-schnaq-to-hub)
        (DELETE "/:keycloak-name/remove" [] remove-schnaq-from-hub)))
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))