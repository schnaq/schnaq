(ns schnaq.api.hub
  (:require [keycloak.admin :as kc-admin]
            [ring.util.http-response :refer [ok forbidden bad-request]]
            [schnaq.auth :as auth]
            [schnaq.config.keycloak :as kc-config :refer [kc-client]]
            [schnaq.database.hub :as hub-db]
            [schnaq.database.main :refer [fast-pull transact]]
            [schnaq.database.user :as user-db]
            [schnaq.processors :as processors]
            [schnaq.validator :as validators]))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [{:keys [identity parameters]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/hub-by-keycloak-name keycloak-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub
             :hub-members (user-db/members-of-group keycloak-name)}))
      (forbidden "You are not allowed to access this ressource."))))

(defn- all-hubs-for-user
  "Return all valid hubs for a user."
  [request]
  (let [keycloak-names (get-in request [:identity :groups])
        keycloak-names (hub-db/create-hubs-if-not-existing keycloak-names)
        hubs (hub-db/hubs-by-keycloak-names keycloak-names)
        processed-hubs (map
                         #(update % :hub/schnaqs
                                  (fn [hub] (map processors/add-meta-info-to-schnaq hub)))
                         hubs)]
    (ok {:hubs processed-hubs})))

(defn- add-schnaq-to-hub
  "Adds a schnaq to a hub identified by the group-name. Only allow the adding when
  the schnaq is not exclusively tied to another hub. Also check for appropriate group membership."
  [{:keys [parameters identity]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        share-hash (get-in parameters [:body :share-hash])]
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
  [{:keys [parameters identity]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        share-hash (get-in parameters [:body :share-hash])]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/remove-discussion-from-hub [:hub/keycloak-name keycloak-name]
                                                   [:discussion/share-hash share-hash])
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      (forbidden {:message "You are not a member of the group."}))))

(defn- change-hub-name
  "Change hub name."
  [{:keys [parameters identity]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        new-hub-name (get-in parameters [:body :new-hub-name])]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/change-hub-name keycloak-name new-hub-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      (forbidden {:message "You are not a member of the hub."}))))

(defn- add-member-to-hub
  "Add a member to a hub using their email-address. If the user is already a member
  nothing should change. If the user is not registered yet, return an appropriate status."
  [{:keys [parameters identity]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        new-member-mail (get-in parameters [:body :new-member-mail])]
    (if (auth/member-of-group? identity keycloak-name)
      (if-let [new-user-keycloak-id (:user.registered/keycloak-id (user-db/user-by-email new-member-mail))]
        (let [group-id (kc-admin/get-group-id kc-client kc-config/realm keycloak-name)]
          (try
            (transact [[:db/add [:user.registered/keycloak-id new-user-keycloak-id]
                        :user.registered/groups keycloak-name]])
            (kc-admin/add-user-to-group! kc-client kc-config/realm group-id new-user-keycloak-id)
            (ok {:status :user-added})
            (catch Exception _e
              (ok {:status :error-adding-user}))))
        (ok {:status :user-not-registered}))
      (forbidden {:message "You are not allowed to add new members to the hub"}))))


;; -----------------------------------------------------------------------------

(def hub-routes
  [["/hubs/personal" {:swagger {:tags ["hubs"]}
                      :middleware [auth/auth-middleware]
                      :get all-hubs-for-user}]
   ["/hub" {:swagger {:tags ["hubs"]}
            :middleware [auth/auth-middleware]}
    ["/:keycloak-name" {:parameters {:path {:keycloak-name :hub/keycloak-name}}}
     ["" {:get hub-by-keycloak-name}]
     ["/add" {:post add-schnaq-to-hub
              :parameters {:body {:share-hash :discussion/share-hash}}}]
     ["/add-member" {:post add-member-to-hub
                     :parameters {:body {:new-member-mail :user.registered/email}}}]
     ["/name" {:put change-hub-name
               :parameters {:body {:new-hub-name :hub/name}}}]
     ["/remove" {:delete remove-schnaq-from-hub
                 :parameters {:body {:share-hash :discussion/share-hash}}}]]]])