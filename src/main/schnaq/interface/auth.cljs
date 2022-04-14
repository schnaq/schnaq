(ns schnaq.interface.auth
  (:require ["keycloak-js" :as Keycloak]
            [cljs.core.async :refer [go <! timeout]]
            [com.fulcrologic.guardrails.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.config :as config]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.modal :as modal]
            [taoensso.timbre :as log]))

(def ^:private refresh-token-time
  "Seconds until the token should be refreshed."
  30)

(defn- error-to-console
  "Shorthand function to log to console."
  [message]
  (rf/dispatch [:ajax.error/to-console message]))

(defn user-authenticated?
  "Quickly lookup authentication status of user."
  [db]
  (get-in db [:user :authenticated?] false))

(defn- request-login-modal
  "Show a modal requesting the user to login again. Can be used for 
   error-handling, e.g. when a token expired and could not be refreshed"
  []
  (let [keycloak @(rf/subscribe [:keycloak/object])]
    [modal/modal-template
     (labels :auth.modal.request-login/title)
     [:<>
      [:p (labels :auth.modal.request-login/lead) " 👍"]
      [:p.text-center
       [:a.btn.btn-outline-secondary
        {:href (.createLoginUrl keycloak)}
        (labels :auth.modal.request-login/button)]]
      [:p
       [:small.text-muted
        [icon :info "my-auto me-1"]
        (labels :auth.modal.request-login/info)]]]]))

;; -----------------------------------------------------------------------------
;; Init function of keycloak. Called in the beginning to check if the user was
;; logged in. Restores the last login state according to the settings in
;; keycloak.

(rf/reg-event-fx
 :keycloak/init
 (fn [{:keys [db]} [_ _]]
   (let [keycloak (Keycloak (clj->js config/keycloak))]
     {:db (assoc-in db [:user :keycloak] keycloak)
      :fx [[:keycloak/silent-check keycloak]]})))

(rf/reg-event-fx
 :keycloak.init/after-login
 (fn [_ [_ authenticated?]]
   {:fx [[:dispatch [:user/authenticated! authenticated?]]
         [:dispatch [:keycloak/load-user-profile]]
         [:dispatch [:keycloak.roles/extract]]
         [:dispatch [:keycloak/check-token-validity]]
         [:dispatch [:user/register]]
         [:dispatch [:hubs.personal/load]]]}))

(rf/reg-fx
 :keycloak/silent-check
 (fn [keycloak]
   (-> keycloak
       (.init #js{:onLoad "check-sso"
                  :checkLoginIframe false
                  :silentCheckSsoRedirectUri (str (-> js/window .-location .-origin) "/silent-check-sso.html")})
       (.then (fn [result]
                (rf/dispatch [:keycloak.init/after-login result])))
       (.catch (fn [_]
                 (rf/dispatch [:user/authenticated! false])
                 (error-to-console "Silent check with keycloak failed."))))))

;; -----------------------------------------------------------------------------
;; Login request to the keycloak instance.

(rf/reg-event-fx
 :keycloak/login
 (fn [{:keys [db]} [_ redirect-uri]]
   (let [keycloak (get-in db [:user :keycloak])]
     (when keycloak
       {:fx [[:keycloak/login-request [keycloak redirect-uri]]]}))))

(rf/reg-fx
 :keycloak/login-request
 (fn [[keycloak redirect-uri]]
   (-> keycloak
       (.login #js {:redirectUri redirect-uri})
       (.catch #(rf/dispatch [:modal {:show? true :child [request-login-modal]}])))))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :keycloak/register
 (fn [{:keys [db]} [_ redirect-uri]]
   (let [keycloak (get-in db [:user :keycloak])]
     (when keycloak
       {:fx [[:keycloak/register-request [keycloak redirect-uri]]]}))))

(rf/reg-fx
 :keycloak/register-request
 (fn [[keycloak redirect-uri]]
   (-> keycloak
       (.register #js {:redirectUri redirect-uri})
       (.catch #(rf/dispatch [:modal {:show? true :child [request-login-modal]}])))))

;; -----------------------------------------------------------------------------
;; If login / init was successful, ask in keycloak for the user's information.

(rf/reg-event-fx
 :keycloak/load-user-profile
 (fn [{:keys [db]} _]
   (let [keycloak (get-in db [:user :keycloak])]
     (when (and keycloak (user-authenticated? db))
       {:fx [[:keycloak/load-user-profile-request keycloak]]}))))

(rf/reg-fx
 :keycloak/load-user-profile-request
 (fn [keycloak]
   (-> keycloak
       (.loadUserProfile)
       (.then #(let [keycloak-fields (js->clj % :keywordize-keys true)]
                 (rf/dispatch [:keycloak/store-groups keycloak-fields])
                 (matomo/set-user-id (:id keycloak-fields))))
       (.catch #(rf/dispatch [:modal {:show? true :child [request-login-modal]}])))))

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
   (let [keycloak (get-in db [:user :keycloak])]
     (when keycloak
       {:fx [[:keycloak/logout-request keycloak]]}))))

(rf/reg-fx
 :keycloak/logout-request
 (fn [keycloak]
   (-> keycloak
       (.logout)
       (.then (fn [_]
                (rf/dispatch [:user/authenticated! false])
                (matomo/reset-user-id)))
       (.catch
        #(error-to-console
          "Logout not successful. Request could not be fulfilled.")))))

;; -----------------------------------------------------------------------------
;; Refresh access token (the one, which is sent to the backend and stored in
;; keycloak.token) when it is expired.

(rf/reg-event-fx
 :keycloak/check-token-validity
 (fn [{:keys [db]} [_ _]]
   (let [keycloak (get-in db [:user :keycloak])]
     (when (and keycloak (user-authenticated? db))
       {:fx [[:keycloak/loop-token-validity-check keycloak]]}))))

(rf/reg-fx
 :keycloak/loop-token-validity-check
 (fn [keycloak]
   (go (while true
         (<! (timeout (* 1000 refresh-token-time)))
         (-> keycloak
             (.updateToken refresh-token-time)
             (.then
              #(when %
                 (log/trace "Access Token for user validation refreshed")))
             (.catch
              (fn [e]
                (rf/dispatch [:modal {:show? true :child [request-login-modal]}])
                (log/error "Error when updating the keycloak access token." e))))))))

(defn- authorization-header [token]
  {:Authorization (gstring/format "Token %s" token)})

(>defn authentication-header
  "Adds a map containing the token used for authenticating the user in the
  backend."
  [db]
  [map? :ret map?]
  (let [keycloak (get-in db [:user :keycloak])
        external-jwt (get-in db [:user :jwt])]
    (cond
      (not (user-authenticated? db)) {}
      keycloak (authorization-header (.-token keycloak))
      external-jwt (authorization-header external-jwt)
      :else {})))

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
 :user/analytics-admin?
 ;; Users that are allowed to see all analytics.
 (fn [db _]
   (let [roles (get-in db [:user :roles])]
     (string? (some shared-config/analytics-roles roles)))))

(rf/reg-sub
 :user/beta-tester?
 (fn [db _]
   (let [roles (get-in db [:user :roles])]
     (string? (some shared-config/beta-tester-roles roles)))))

(rf/reg-sub
 :user/subscription
 (fn [db _]
   (get-in db [:user :subscription])))

(rf/reg-sub
 :user/pro-user?
 :<- [:user/beta-tester?]
 :<- [:user/subscription]
 (fn [[beta-tester? subscription]]
   (or beta-tester?
       (= :user.registered.subscription.type/pro (:type subscription)))))

(rf/reg-event-db
 :keycloak.roles/extract
 (fn [db [_ _]]
   (when (user-authenticated? db)
     (let [keycloak (get-in db [:user :keycloak])
           roles (:roles (js->clj (oget keycloak [:realmAccess])
                                  :keywordize-keys true))]
       (assoc-in db [:user :roles] roles)))))

(rf/reg-sub
 :keycloak/object
 (fn [db]
   (get-in db [:user :keycloak])))
