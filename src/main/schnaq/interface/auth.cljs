(ns schnaq.interface.auth
  (:require ["keycloak-js" :as Keycloak]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]))

(defn- error-to-console
  "Shorthand function to log to console."
  [message]
  (rf/dispatch [:ajax.error/to-console message]))

(defn- user-authenticated?
  "Quickly lookup authentication status of user."
  [app-db]
  (get-in app-db [:user :authenticated?] false))


;; -----------------------------------------------------------------------------
;; Init function of keycloak. Called in the beginning to check if the user was
;; logged in. Restores the last login state according to the settings in
;; keycloak.

(rf/reg-event-fx
  :keycloak/init
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (Keycloak (clj->js config/keycloak))]
      {:db (assoc-in db [:user :keycloak] keycloak)
       :fx [[:keycloak/silent-check keycloak]]})))

(rf/reg-fx
  :keycloak/silent-check
  (fn [^js keycloak]
    (-> keycloak
        (.init #js{:onLoad "check-sso"
                   :checkLoginIframe false
                   :silentCheckSsoRedirectUri (str (-> js/window .-location .-origin) "/silent-check-sso.html")})
        (.then (fn [result]
                 (rf/dispatch [:user/authenticated! result])
                 (rf/dispatch [:keycloak/load-user-profile])
                 (rf/dispatch [:keycloak.roles/extract])))
        (.catch (fn [_]
                  (rf/dispatch [:user/authenticated! false])
                  (error-to-console "Silent check with keycloak failed."))))))


;; -----------------------------------------------------------------------------
;; Login request to the keycloak instance.

(rf/reg-event-fx
  :keycloak/login
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (get-in db [:user :keycloak])]
      (when keycloak
        {:fx [[:keycloak/login-request keycloak]]}))))

(rf/reg-fx
  :keycloak/login-request
  (fn [keycloak]
    (-> keycloak
        (.login)
        (.then #(rf/dispatch [:keycloak/load-user-profile]))
        (.catch #(error-to-console
                   "Login not successful. Request could not be fulfilled.")))))


;; -----------------------------------------------------------------------------
;; If login / init was successful, ask in keycloak for the user's information.

(rf/reg-event-fx
  :keycloak/load-user-profile
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (get-in db [:user :keycloak])]
      (when (and keycloak (user-authenticated? db))
        {:fx [[:keycloak/load-user-profile-request keycloak]]}))))

(rf/reg-fx
  :keycloak/load-user-profile-request
  (fn [^js keycloak]
    (-> keycloak
        (.loadUserProfile)
        (.then #(rf/dispatch [:keycloak/store-user-profile
                              (js->clj % :keywordize-keys true)]))
        (.catch #(error-to-console
                   "Could not load user profile from keycloak.")))))

(rf/reg-event-db
  :keycloak/store-user-profile
  (fn [db [_ {:keys [username email]}]]
    (-> db
        (assoc-in [:user :name] username)
        (cond-> email
                (assoc-in [:user :email] email)))))


;; -----------------------------------------------------------------------------
;; Logout functions.

(rf/reg-event-fx
  :keycloak/logout
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (get-in db [:user :keycloak])]
      (when keycloak
        {:fx [[:keycloak/logout-request keycloak]]}))))

(rf/reg-fx
  :keycloak/logout-request
  (fn [keycloak]
    (-> keycloak
        (.logout)
        (.then #(rf/dispatch [:user/authenticated! false]))
        (.catch
          #(error-to-console
             "Logout not successful. Request could not be fulfilled.")))))


;; -----------------------------------------------------------------------------

(rf/reg-event-db
  :user/authenticated!
  (fn [db [_ toggle]]
    (assoc-in db [:user :authenticated?] toggle)))

(rf/reg-sub
  :user/authenticated?
  (fn [db _]
    (get-in db [:user :authenticated?] false)))

(rf/reg-sub
  :user/administrator?
  (fn [db _]
    (let [roles (get-in db [:user :roles])]
      (= :admin (some #{:admin} roles)))))

(rf/reg-sub
  :user/keycloak
  (fn [db _]
    (get-in db [:user :keycloak])))

(rf/reg-event-db
  :keycloak.roles/extract
  (fn [db [_ _]]
    (when (user-authenticated? db)
      (let [^js keycloak (get-in db [:user :keycloak])
            roles (:roles (js->clj (oget keycloak [:realmAccess])
                                   :keywordize-keys true))]
        (assoc-in db [:user :roles] (mapv keyword roles))))))
