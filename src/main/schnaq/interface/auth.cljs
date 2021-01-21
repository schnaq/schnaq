(ns schnaq.interface.auth
  (:require ["keycloak-js" :as Keycloak]
            [schnaq.interface.config :as config]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

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
                 (rf/dispatch [:keycloak/load-user-profile])))
        (.catch #(rf/dispatch [:user/authenticated! false])))))


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
        (.catch
          (fn [error]
            (rf/dispatch
              [:notification/add
               #:notification{:title "Login fehlgeschlagen"
                              :body [:pre [:code (str error)]]
                              :context :danger
                              :stay-visible? true
                              :on-close-fn #(rf/dispatch [:clear-error])}]))))))


;; -----------------------------------------------------------------------------
;; If login / init was successful, ask in keynote for the user's information.

(rf/reg-event-fx
  :keycloak/load-user-profile
  (fn [{:keys [db]} [_ _]]
    (let [^js keycloak (get-in db [:user :keycloak])]
      (when keycloak
        {:fx [[:keycloak/load-user-profile-request keycloak]]}))))

(rf/reg-fx
  :keycloak/load-user-profile-request
  (fn [^js keycloak]
    (-> keycloak
        (.loadUserProfile)
        (.then #(rf/dispatch [:keycloak/store-user-profile
                              (js->clj % :keywordize-keys true)])))))

(rf/reg-event-db
  :keycloak/store-user-profile
  (fn [db [_ {:keys [username email] :as profile}]]
    (prn profile)
    (-> db
        (assoc-in [:user :name] username)
        (cond-> email
                (assoc-in [:user :email] email)))))


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
  :user/keycloak
  (fn [db _]
    (get-in db [:user :keycloak])))

(comment
  (rf/dispatch [:keycloak/login])
  (rf/dispatch [:keycloak/init])
  (rf/dispatch [:keycloak/check-state])
  (let [profile
        (-> @re-frame.db/app-db
            :user :keycloak
            .loadUserProfile)]
    (.then profile (fn [e] (prn (js->clj e)))))
  (.log js/console keycloak)
  (.init keycloak))