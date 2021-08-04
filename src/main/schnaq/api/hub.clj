(ns schnaq.api.hub
  (:require [clojure.spec.alpha :as s]
            [keycloak.admin :as kc-admin]
            [ring.util.http-response :refer [ok forbidden not-found internal-server-error]]
            [schnaq.api.toolbelt :as at]
            [schnaq.auth :as auth]
            [schnaq.config.keycloak :as kc-config :refer [kc-client]]
            [schnaq.database.hub :as hub-db]
            [schnaq.database.main :refer [fast-pull transact]]
            [schnaq.database.specs :as specs]
            [schnaq.database.user :as user-db]
            [schnaq.media :as media]
            [schnaq.processors :as processors]
            [schnaq.validator :as validators]
            [taoensso.timbre :as log]))

(def ^:private forbidden-missing-permission
  (forbidden (at/build-error-body :hub/not-a-member "You are not a member of the hub.")))

(defn- hub-by-keycloak-name
  "Query hub by its referenced name in keycloak."
  [{:keys [identity parameters]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/hub-by-keycloak-name keycloak-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub
             :hub-members (user-db/members-of-group keycloak-name)}))
      forbidden-missing-permission)))

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
        (not-found (at/build-error-body :hub/discussion-not-found
                                        "The discussion could not be found.")))
      forbidden-missing-permission)))

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
      forbidden-missing-permission)))

(defn- change-hub-name
  "Change hub name."
  [{:keys [parameters identity]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        new-hub-name (get-in parameters [:body :new-hub-name])]
    (if (auth/member-of-group? identity keycloak-name)
      (let [hub (hub-db/change-hub-name keycloak-name new-hub-name)
            processed-hub (update hub :hub/schnaqs #(map processors/add-meta-info-to-schnaq %))]
        (ok {:hub processed-hub}))
      forbidden-missing-permission)))

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
              (internal-server-error (at/build-error-body :hub/error-adding-user
                                                          "User could not be added")))))
        (ok {:status :user-not-registered}))
      forbidden-missing-permission)))

(defn- change-hub-logo
  "Change the hub's logo.
  This includes uploading an image to s3 and updating the associated url in the database."
  [{:keys [identity parameters]}]
  (let [keycloak-name (get-in parameters [:path :keycloak-name])
        image-type (get-in parameters [:body :image :type])
        image-name (get-in parameters [:body :image :name])
        image-content (get-in parameters [:body :image :content])]
    (log/info (format "User %s is trying to set logo of Hub %s to: %s" (:id identity) keycloak-name image-name))
    (if (auth/member-of-group? identity keycloak-name)
      (let [{:keys [image-url] :as response} (media/upload-image! image-type image-content :hub/logo)]
        (if image-url
          (ok {:hub (hub-db/update-hub-logo-url keycloak-name image-url)})
          response))
      forbidden-missing-permission)))


;; -----------------------------------------------------------------------------

(def hub-routes
  [["" {:swagger {:tags ["hubs"]}
        :middleware [:user/authenticated?]
        :responses {401 at/response-error-body
                    403 at/response-error-body}}
    ["/hubs/personal" {:get all-hubs-for-user
                       :description (at/get-doc #'all-hubs-for-user)
                       :name :hubs/personal
                       :responses {200 {:body {:hubs (s/coll-of ::specs/hub)}}}}]
    ["/hub/:keycloak-name" {:parameters {:path {:keycloak-name :hub/keycloak-name}}}
     ["" {:get hub-by-keycloak-name
          :description (at/get-doc #'hub-by-keycloak-name)
          :name :hub/by-name
          :responses {200 {:body {:hub ::specs/hub
                                  :hub-members (s/coll-of ::specs/any-user)}}}}]
     ["/add" {:post add-schnaq-to-hub
              :description (at/get-doc #'add-schnaq-to-hub)
              :name :hub/add-schnaq
              :parameters {:body {:share-hash :discussion/share-hash}}
              :responses {200 {:body {:hub ::specs/hub}}
                          404 at/response-error-body}}]
     ["/name" {:put change-hub-name
               :description (at/get-doc #'change-hub-name)
               :name :hub/change-name
               :parameters {:body {:new-hub-name :hub/name}}
               :responses {200 {:body {:hub ::specs/hub}}}}]
     ["/logo" {:put change-hub-logo
               :description (at/get-doc #'change-hub-logo)
               :name :hub/change-logo
               :parameters {:body {:image ::specs/image}}
               :responses {200 {:body {:hub ::specs/hub}}
                           400 at/response-error-body}}]
     ["/remove" {:delete remove-schnaq-from-hub
                 :description (at/get-doc #'remove-schnaq-from-hub)
                 :name :hub/remove-schnaq
                 :parameters {:body {:share-hash :discussion/share-hash}}
                 :responses {200 {:body {:hub ::specs/hub}}}}]
     ["/member/add" {:post add-member-to-hub
                     :description (at/get-doc #'add-member-to-hub)
                     :name :hub/add-member
                     :parameters {:body {:new-member-mail :user.registered/email}}
                     :responses {200 {:body {:status keyword?}}}}]]]])
