(ns schnaq.interface.user
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.http :as http]))

(rf/reg-event-fx
 :username/from-localstorage
 (fn [{:keys [db]} _]
   (when-let [username (:username local-storage)]
     {:db (assoc-in db [:user :names :display] username)})))

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
                                 (cond-> {}
                                   visited-hashes (assoc :visited-hashes visited-hashes)
                                   visited-statements (assoc :visited-statement-ids visited-statements)
                                   creation-secrets (assoc :creation-secrets creation-secrets)
                                   schnaq-creation-secrets (assoc :schnaq-creation-secrets schnaq-creation-secrets)))]}))))

(rf/reg-event-fx
 :user.register/success
 (fn [{:keys [db]} [_ {:keys [registered-user updated-statements? updated-schnaqs?]}]]
   (let [{:user.registered/keys [display-name first-name last-name email profile-picture visited-schnaqs keycloak-id notification-mail-interval]} registered-user
         subscription-type (:user.registered.subscription/type registered-user)
         current-route-name (navigation/canonical-route-name (get-in db [:current-route :data :name]))
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         visited-hashes (map :discussion/share-hash visited-schnaqs)]
     {:db (-> db
              (assoc-in [:user :names :display] display-name)
              (assoc-in [:user :email] email)
              (assoc-in [:user :id] (:db/id registered-user))
              (assoc-in [:user :keycloak-id] keycloak-id)
              (assoc-in [:user :profile-picture :display] profile-picture)
              (cond-> notification-mail-interval (assoc-in [:user :notification-mail-interval] notification-mail-interval))
              (cond-> first-name (assoc-in [:user :names :first] first-name))
              (cond-> last-name (assoc-in [:user :names :last] last-name))
              (cond-> subscription-type (assoc-in [:user :subscription :type] subscription-type))
              ;; Clear secrets, they have been persisted.
              (assoc-in [:discussion :statements :creation-secrets] {})
              (assoc-in [:discussion :schnaqs :creation-secrets] {}))
      :fx [[:localstorage/dissoc :discussion/creation-secrets]
           [:localstorage/dissoc :discussion.schnaqs/creation-secrets]
           [:dispatch [:schnaqs.visited/merge-registered-users-visits visited-hashes]]
           (when (and updated-statements? (= current-route-name :routes.schnaq.select/statement))
             ;; The starting-statement view is updated automatically anyway
             [:dispatch [:discussion.query.statement/by-id]])
           (when (and updated-schnaqs? (= current-route-name :routes.schnaq/start))
             [:dispatch [:schnaq/load-by-share-hash share-hash]])]})))

(rf/reg-sub
 :user/id
 (fn [db _]
   (get-in db [:user :id])))

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

(rf/reg-sub
 :user/currency
 (fn [db]
   (get-in db [:user :currency])))

(rf/reg-sub
 :user.currency/symbol
 :<- [:user/currency]
 (fn [currency]
   (if (= :usd currency)
     "$"
     "€")))
