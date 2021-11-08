(ns schnaq.interface.events
  (:require [goog.string :as gstring]
            [goog.userAgent :as gagent]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit-frontend]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.routes :as routes]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.language :as lang]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

;; Note: this lives in the common namespace to prevent circles through the routes import
(rf/reg-event-fx
  :hub.schnaqs/add
  (fn [{:keys [db]} [_ form]]
    (let [schnaq-input (oget form :schnaq-add-input :value)
          share-hash (or @(rf/subscribe [:schnaq/share-hash])
                         schnaq-input)
          keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      {:fx [(http/xhrio-request db :post (gstring/format "/hub/%s/add" keycloak-name)
                                [:hub.schnaqs/add-success form]
                                {:share-hash share-hash}
                                [:hub.schnaqs/add-failure])]})))

(rf/reg-event-fx
  :load/last-added-schnaq
  (fn [_ _]
    (let [share-hash (:schnaq.last-added/share-hash local-storage)
          edit-hash (:schnaq.last-added/edit-hash local-storage)]
      (when-not (and (nil? edit-hash) (nil? share-hash))
        {:fx [[:dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash]]]}))))

(rf/reg-event-fx
  :re-frame-10x/hide-on-mobile
  (fn [_ _]
    (when gagent/MOBILE
      {:fx [[:localstorage/assoc
             ['day8.re-frame-10x.show-panel false]]]})))

(rf/reg-event-fx
  :initialize/schnaq
  (fn [_ _]
    {:fx [[:dispatch [:username/from-localstorage]]
          [:dispatch [:re-frame-10x/hide-on-mobile]]
          [:dispatch [:how-to-visibility/from-localstorage-to-app-db]]
          [:dispatch [:keycloak/init]]
          [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]
          [:dispatch [:visited.save-statement-ids/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.save-admin-access/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]
          [:dispatch [:schnaq.discussion-secrets/load-from-localstorage]]
          [:dispatch [:load/last-added-schnaq]]]}))

(rf/reg-event-fx
  :form/should-clear
  (fn [_ [_ form-elements]]
    {:fx [[:form/clear form-elements]]}))

(rf/reg-fx
  :form/clear
  (fn [form-elements]
    (toolbelt/reset-form-fields! form-elements)))

(rf/reg-sub
  :current-locale
  (fn [db _]
    (get db :locale)))

(rf/reg-sub
  :current-language
  (fn [_ _]
    (rf/subscribe [:current-locale]))
  (fn [locale _]
    (case locale
      :de "Deutsch"
      :en "English"
      :pl "Polski")))

(rf/reg-fx
  ;; Changes the HTML lang attribute accordingly.
  :change-document-lang
  (fn [lang-short]
    (let [locale-string (case lang-short
                          :de "de-DE"
                          :en "en-US"
                          :pl "pl-PL"
                          "de-DE")]
      (.setAttribute (.-documentElement js/document) "lang" locale-string))))

(rf/reg-event-fx
  :set-locale
  (fn [{:keys [db]} [_ locale]]
    {:db (assoc db :locale locale)
     :fx [[:change-document-lang locale]]}))

(rf/reg-fx
  ;; Changes location via js, lets reitit re-match the url after changing
  :change-location
  (fn [url]
    (rf/dispatch
      [:navigation/navigated
       (reitit-frontend/match-by-path routes/router (str (-> js/window .-location .-origin) "/" url))])))

(rf/reg-fx
  ;; Changes more than just the document locale, like changing the key in config and writing it to localstorage.
  ;; (But includes execution of set-locale)
  :switch-language
  (fn [locale]
    (lang/set-language locale)))

(rf/reg-event-fx
  :language/set-and-redirect
  (fn [_ [_ locale redirect-url]]
    {:fx [[:switch-language locale]
          [:change-location redirect-url]]}))

(rf/reg-event-fx
  :schnaq/select-current-from-backend
  (fn [_ [_ {:keys [schnaq]}]]
    {:fx [[:dispatch [:schnaq/select-current schnaq]]]}))

(rf/reg-event-fx
  :schnaq/select-current
  (fn [{:keys [db]} [_ {:discussion/keys [share-hash edit-hash] :as schnaq}]]
    (let [admin-access-map (get-in db [:schnaqs :admin-access] {})
          updated-access-map (if edit-hash (assoc admin-access-map share-hash edit-hash) admin-access-map)
          edit-hash-localstorage (or edit-hash (get admin-access-map share-hash))]
      {:db (cond->
             db
             true (assoc-in [:schnaq :selected] schnaq)
             edit-hash (assoc-in [:schnaqs :admin-access] updated-access-map)
             edit-hash-localstorage (assoc-in [:schnaq :selected :discussion/edit-hash] edit-hash-localstorage))
       :fx [[:dispatch [:schnaq.visited/to-localstorage share-hash]]
            [:localstorage/assoc [:schnaqs/admin-access updated-access-map]]]})))

(rf/reg-sub
  :schnaq/selected
  (fn [db _]
    (get-in db [:schnaq :selected])))

(rf/reg-sub
  :schnaq/share-hash
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (:discussion/share-hash selected-schnaq)))

(rf/reg-sub
  :schnaq/edit-hash
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (:discussion/edit-hash selected-schnaq)))

(rf/reg-sub
  :schnaq.selected/statement-number
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (-> selected-schnaq :meta-info :all-statements)))

(rf/reg-sub
  :schnaq.mode/qanda?
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (= :discussion.mode/qanda (:discussion/mode selected-schnaq))))

(rf/reg-sub
  :schnaq.selected/access-code
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (get-in selected-schnaq [:discussion/access :discussion.access/code])))

(rf/reg-sub
  :schnaq.selected/read-only?
  (fn [_ _]
    (rf/subscribe [:schnaq/selected]))
  (fn [selected-schnaq _ _]
    (not (nil? (some #{:discussion.state/read-only} (:discussion/states selected-schnaq))))))

(rf/reg-event-fx
  :schnaq/load-by-share-hash
  ;; Explicit api-url is optional
  (fn [{:keys [db]} [_ share-hash api-url]]
    (let [request (partial http/xhrio-request db :get "/schnaq/by-hash" [:schnaq/select-current-from-backend]
                           {:share-hash share-hash
                            :display-name (toolbelt/current-display-name db)})]
      {:db (assoc-in db [:schnaq :selected :discussion/share-hash] share-hash)
       :fx [(if api-url
              (request [:ajax.error/to-console] api-url)
              (request))]})))

(rf/reg-event-fx
  :schnaq/add-visited!
  (fn [{:keys [db]} [_ share-hash]]
    (when (auth/user-authenticated? db)
      {:fx [(http/xhrio-request db :put "/schnaq/add-visited"
                                [:no-op]
                                {:share-hash share-hash})]})))

(rf/reg-event-fx
  :schnaq/refresh-selected
  ;;Refreshes the selected schnaq by reloading it from the backend
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:fx [[:dispatch [:schnaq/load-by-share-hash share-hash]]]})))

(rf/reg-event-fx
  :schnaq/check-admin-credentials
  (fn [{:keys [db]} [_ share-hash edit-hash]]
    {:fx [(http/xhrio-request db :post "/credentials/validate" [:schnaq/check-admin-credentials-success]
                              {:share-hash share-hash
                               :edit-hash edit-hash}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-fx
  ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
  ;; the backend every time)
  :schnaq/check-admin-credentials-success
  (fn [_ [_ {:keys [valid-credentials?]}]]
    (when-not valid-credentials?
      {:fx [[:dispatch [:navigation/navigate :routes/forbidden-page]]]})))

(rf/reg-event-db
  :schnaq/save-as-last-added
  (fn [db [_ {:keys [schnaq]}]]
    (assoc-in db [:schnaq :last-added] schnaq)))

(rf/reg-sub
  :schnaq/last-added
  (fn [db _]
    (get-in db [:schnaq :last-added])))

(rf/reg-event-fx
  :schnaq/load-by-hash-as-admin
  (fn [{:keys [db]} [_ share-hash edit-hash]]
    {:fx [(http/xhrio-request db :post "/schnaq/by-hash-as-admin" [:schnaq/save-as-last-added]
                              {:share-hash share-hash
                               :edit-hash edit-hash})]}))
