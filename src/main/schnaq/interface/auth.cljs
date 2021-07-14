(ns schnaq.interface.auth
  (:require ["keycloak-js" :as Keycloak]
            [cljs.core.async :refer [go <! timeout]]
            [ghostwheel.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :as config]
            [taoensso.timbre :as log]))

(defn- error-to-console
  "Shorthand function to log to console."
  [message]
  (rf/dispatch [:ajax.error/to-console message]))

(defn user-authenticated?
  "Quickly lookup authentication status of user."
  [db]
  (get-in db [:user :authenticated?] false))


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
                 (rf/dispatch [:keycloak.roles/extract])
                 (rf/dispatch [:keycloak/check-token-validity])
                 (rf/dispatch [:user/register result])
                 (rf/dispatch [:hubs.personal/load])))
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
  (fn [^js keycloak]
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
        (.then #(rf/dispatch [:keycloak/store-groups
                              (js->clj % :keywordize-keys true)]))
        (.catch #(error-to-console
                   "Could not load user profile from keycloak.")))))

(rf/reg-event-db
  :keycloak/store-groups
  (fn [db _]
    (let [keycloak (get-in db [:user :keycloak])
          groups (js->clj (oget keycloak [:tokenParsed :groups]))]
      (when (seq groups)
        (assoc-in db [:user :groups] groups)))))


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
  (fn [^js keycloak]
    (-> keycloak
        (.logout)
        (.then #(rf/dispatch [:user/authenticated! false]))
        (.catch
          #(error-to-console
             "Logout not successful. Request could not be fulfilled.")))))


;; -----------------------------------------------------------------------------
;; Refresh access token (the one, which is sent to the backend and stored in
;; keycloak.token) when it is expired.

(rf/reg-event-fx
  :keycloak/check-token-validity
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (get-in db [:user :keycloak])
          authenticated? (get-in db [:user :authenticated?])]
      (when (and keycloak authenticated?)
        {:fx [[:keycloak/loop-token-validity-check keycloak]]}))))

(rf/reg-fx
  :keycloak/loop-token-validity-check
  (fn [^js keycloak]
    (go (while true
          (<! (timeout 30000))
          (-> keycloak
              (.updateToken 30)
              (.then
                #(when %
                   (log/trace "Access Token for user validation refreshed")))
              (.catch
                #(log/error
                   "Error when updating the keycloak access token.")))))))


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
      (string? (some shared-config/admin-roles roles)))))

(rf/reg-sub
  :user/beta-tester?
  (fn [db _]
    (let [roles (get-in db [:user :roles])]
      (string? (some shared-config/beta-tester-roles roles)))))

(rf/reg-event-db
  :keycloak.roles/extract
  (fn [db [_ _]]
    (when (user-authenticated? db)
      (let [^js keycloak (get-in db [:user :keycloak])
            roles (:roles (js->clj (oget keycloak [:realmAccess])
                                   :keywordize-keys true))]
        (assoc-in db [:user :roles] roles)))))

(>defn authentication-header
  "Adds a map containing the token used for authenticating the user in the
  backend."
  [db]
  [map? :ret map?]
  (let [^js keycloak (get-in db [:user :keycloak])]
    (if (and keycloak (user-authenticated? db))
      {:Authorization (gstring/format "Token %s" (.-token keycloak))}
      {})))
