(ns schnaq.interface.user
  (:require ["unique-names-generator" :refer [uniqueNamesGenerator, colors, animals]]
            [clojure.string :as clj-string]
            [re-frame.core :as rf]
            [schnaq.config.shared :refer [default-anonymous-display-name] :as shared-config]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
            [schnaq.interface.utils.toolbelt :as tools]))

(defn- random-name
  "Generate a random name for anonymous users."
  []
  (uniqueNamesGenerator #js {:dictionaries #js [colors animals]
                             :separator " "
                             :style "capital"}))

(rf/reg-event-fx
 :username/generate-or-load
 ;; Either load a name from localstorage, or if it is a new user generate a random name and set it.
 (fn [_ _]
   (if-let [username (from-localstorage :username)]
     {:fx [[:dispatch [:user.name/store username]]]}
     {:fx [[:dispatch [:user/set-display-name (random-name)]]]})))

(rf/reg-event-fx
 :user/init-device-id
 (fn [{:keys [db]} _]
   (if-let [device-id (from-localstorage :device-id)]
     {:db (assoc-in db [:user :device-id] device-id)}
     (let [new-device-id (random-uuid)]
       {:db (assoc-in db [:user :device-id] new-device-id)
        :fx [[:localstorage/assoc [:device-id new-device-id]]]}))))

(rf/reg-event-fx
 ;; Registers a user in the backend. Sets the returned user in the db
 :user/register
 (fn [{:keys [db]}]
   (when (auth/user-authenticated? db)
     (let [creation-secrets (get-in db [:discussion :statements :creation-secrets])
           visited-hashes (get-in db [:schnaqs :visited-hashes])
           visited-statements (get-in db [:visited :statement-ids] {})]
       {:fx [(http/xhrio-request db :put "/user/register" [:user.register/success]
                                 (cond-> {:locale (get db :locale :en)}
                                   visited-hashes (assoc :visited-hashes visited-hashes)
                                   visited-statements (assoc :visited-statement-ids visited-statements)
                                   creation-secrets (assoc :creation-secrets creation-secrets)))]}))))

(rf/reg-event-fx
 :user.register/success
 (fn [{:keys [db]} [_ {:keys [registered-user updated-statements? new-user? meta]}]]
   (let [{:user.registered/keys [display-name first-name last-name email profile-picture visited-schnaqs archived-schnaqs keycloak-id notification-mail-interval roles]} registered-user
         current-route-name (navigation/canonical-route-name (get-in db [:current-route :data :name]))
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
              (assoc-in [:discussion :statements :creation-secrets] {}))
      :fx [[:localstorage/dissoc :discussion/creation-secrets]
           [:dispatch [:schnaqs.archived-and-visited/to-localstorage visited-hashes archived-hashes]]
           (when new-user?
             [:matomo/track-event ["User Registration" "Registration" "Account Creation Free"]])
           (when (and updated-statements? (= current-route-name :routes.schnaq.select/statement))
             ;; The starting-statement view is updated automatically anyway
             [:dispatch [:discussion.query.statement/by-id]])]})))

;; -----------------------------------------------------------------------------
;; Subscriptions

(rf/reg-sub
 :user/current
 :-> :user)

(rf/reg-sub
 :user/entity
 ;; The user at it was queried from the database
 :<- [:user/current]
 :-> :entity)

(rf/reg-sub
 :user/id
 :<- [:user/current]
 :-> :id)

(rf/reg-sub
 :user/meta
 ;; The user at it was queried from the database
 :<- [:user/current]
 :-> :meta)

(rf/reg-sub
 :user/currency
 :<- [:user/current]
 :-> :currency)

(rf/reg-sub
 :user/profile-picture
 :<- [:user/current]
 (fn [user]
   (get-in user [:profile-picture :display])))

(rf/reg-sub
 :user/roles
 :<- [:user/current]
 :-> :roles)

(rf/reg-sub
 :user/subscription
 :<- [:user/current]
 :-> :subscription)

(rf/reg-sub
 :user.currency/symbol
 :<- [:user/currency]
 (fn [currency]
   (if (= :usd currency)
     "$"
     "€")))

(rf/reg-sub
 :user/display-name
 :-> tools/current-display-name)

(rf/reg-sub
 :user/groups
 :<- [:user/current]
 (fn [user]
   (get user :groups [])))

(rf/reg-sub
 :user/show-display-name-input?
 (fn [db]
   (get-in db [:controls :username-input :show?] false)))

(defn user-moderator?
  "Helper-function to check for moderators."
  [selected-schnaq user-id]
  (or (= user-id (:db/id (:discussion/author selected-schnaq)))
      (contains? (set (:discussion/moderators selected-schnaq)) user-id)))

(rf/reg-sub
 :user/moderator?
 ;; Checks whether the user is moderator of the current schnaq.
 :<- [:schnaq/selected]
 :<- [:user/id]
 (fn [[selected-schnaq user-id] _]
   (user-moderator? selected-schnaq user-id)))

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
   (when-let [currency (from-localstorage :user/currency)]
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
