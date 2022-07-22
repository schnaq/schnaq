(ns schnaq.interface.user
  (:require [clojure.string :as clj-string]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.config.shared :refer [default-anonymous-display-name] :as shared-config]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(rf/reg-event-fx
 :username/from-localstorage
 (fn [_ _]
   (when-let [username (:username local-storage)]
     {:fx [[:dispatch [:user.name/store username]]]})))

(rf/reg-event-fx
 ;; Registers a user in the backend. Sets the returned user in the db
 :user/register
 (fn [{:keys [db]}]
   (when (auth/user-authenticated? db)
     (let [creation-secrets (get-in db [:discussion :statements :creation-secrets])
           schnaq-creation-secrets (get-in db [:discussion :schnaqs :creation-secrets])
           visited-hashes (get-in db [:schnaqs :visited-hashes])
           visited-statements (get-in db [:visited :statement-ids] {})]
       {:fx [(http/xhrio-request db :put "/user/register" [:user.register/success]
                                 (cond-> {:locale (get db :locale :en)}
                                   visited-hashes (assoc :visited-hashes visited-hashes)
                                   visited-statements (assoc :visited-statement-ids visited-statements)
                                   creation-secrets (assoc :creation-secrets creation-secrets)
                                   schnaq-creation-secrets (assoc :schnaq-creation-secrets schnaq-creation-secrets)))]}))))

(rf/reg-event-fx
 :user.register/success
 (fn [{:keys [db]} [_ {:keys [registered-user updated-statements? updated-schnaqs? new-user? meta]}]]
   (let [{:user.registered/keys [display-name first-name last-name email profile-picture visited-schnaqs archived-schnaqs keycloak-id notification-mail-interval roles]} registered-user
         current-route-name (navigation/canonical-route-name (get-in db [:current-route :data :name]))
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         visited-hashes (map :discussion/share-hash visited-schnaqs)
         archived-hashes (map :discussion/share-hash archived-schnaqs)]
     {:db (-> db
              (assoc-in [:user :entity] registered-user)
              (assoc-in [:user :names :display] display-name)
              (assoc-in [:user :email] email)
              (assoc-in [:user :id] (:db/id registered-user))
              (assoc-in [:user :keycloak-id] keycloak-id)
              (assoc-in [:user :profile-picture :display] profile-picture)
              (assoc-in [:user :meta] meta)
              (cond-> roles (assoc-in [:user :roles] roles))
              (cond-> notification-mail-interval (assoc-in [:user :notification-mail-interval] notification-mail-interval))
              (cond-> first-name (assoc-in [:user :names :first] first-name))
              (cond-> last-name (assoc-in [:user :names :last] last-name))
              ;; Clear secrets, they have been persisted.
              (assoc-in [:discussion :statements :creation-secrets] {})
              (assoc-in [:discussion :schnaqs :creation-secrets] {}))
      :fx [[:localstorage/dissoc :discussion/creation-secrets]
           [:localstorage/dissoc :discussion.schnaqs/creation-secrets]
           [:dispatch [:schnaqs.archived-and-visited/to-localstorage visited-hashes archived-hashes]]
           (when new-user?
             [:matomo/track-event ["User Registration" "Registration" "Account Creation Free"]])
           (when (and updated-statements? (= current-route-name :routes.schnaq.select/statement))
             ;; The starting-statement view is updated automatically anyway
             [:dispatch [:discussion.query.statement/by-id]])
           (when (and updated-schnaqs? (= current-route-name :routes.schnaq/start))
             [:dispatch [:schnaq/load-by-share-hash share-hash]])]})))

;; -----------------------------------------------------------------------------
;; Subscriptions

(rf/reg-sub
 :user/current
 (fn [db _] (:user db)))

(rf/reg-sub
 :user/entity
 ;; The user at it was queried from the database
 :<- [:user/current]
 (fn [user]
   (:entity user)))

(rf/reg-sub
 :user/id
 :<- [:user/current]
 (fn [user]
   (:id user)))

(rf/reg-sub
 :user/currency
 :<- [:user/current]
 (fn [user]
   (:currency user)))

(rf/reg-sub
 :user.currency/symbol
 :<- [:user/currency]
 (fn [currency]
   (if (= :usd currency)
     "$"
     "€")))

(rf/reg-sub
 :user/display-name
 (fn [db _]
   (tools/current-display-name db)))

(rf/reg-sub
 :user/groups
 :<- [:user/current]
 (fn [user]
   (get user :groups [])))

(rf/reg-sub
 :user/show-display-name-input?
 (fn [db]
   (get-in db [:controls :username-input :show?] false)))

;; -----------------------------------------------------------------------------
;; Events

(rf/reg-event-fx
 :user.currency/store
 (fn [{:keys [db]} [_ currency]]
   (when (shared-config/currencies currency)
     {:db (assoc-in db [:user :currency] currency)
      :fx [[:localstorage/assoc [:user/currency currency]]]})))

(rf/reg-event-fx
 :user.currency/from-localstorage
 (fn [{:keys [db]} _]
   (when-let [currency (:user/currency local-storage)]
     {:db (assoc-in db [:user :currency] currency)})))

(rf/reg-event-fx
 :user/set-display-name
 (fn [{:keys [db]} [_ username]]
   ;; only update when string contains
   (when-not (clj-string/blank? username)
     (cond-> {:fx [(http/xhrio-request db :put "/user/anonymous/add" [:user/hide-display-name-input username]
                                       {:nickname username}
                                       [:ajax.error/as-notification])
                   [:dispatch [:user.name/store username]]]}
       (not= default-anonymous-display-name username)
       (update :fx conj [:localstorage/assoc [:username username]])))))

(rf/reg-event-fx
 :user/hide-display-name-input
 (fn [{:keys [db]} [_ username]]
   (let [notification
         [[:dispatch [:notification/add
                      #:notification{:title (labels :user.button/set-name)
                                     :body (labels :user.button/success-body)
                                     :context :success}]]]]
     ;; Show notification if user is not default anonymous display name
     (cond-> {:db (assoc-in db [:controls :username-input :show?] false)}
       (not= default-anonymous-display-name username) (assoc :fx notification)))))

(rf/reg-event-db
 :user/show-display-name-input
 (fn [db _]
   (assoc-in db [:controls :username-input :show?] true)))
