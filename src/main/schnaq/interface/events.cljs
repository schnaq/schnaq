(ns schnaq.interface.events
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit-frontend]
            [schnaq.interface.db :as schnaq-db]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.language :as lang]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(rf/reg-event-fx
  :load/schnaqs
  (fn [{:keys [db]} _]
    (when-not toolbelt/production?
      {:fx [(http/xhrio-request db :get "/schnaqs" [:init-from-backend])]})))

(rf/reg-event-fx
  :load/last-added-schnaq
  (fn [_ _]
    ;; PARTIALLY DEPRECATED, deleted after 2021-09-22: Remove old ls/get-item and only use new.
    (let [share-hash (or (:schnaq.last-added/share-hash local-storage)
                         (ls/get-item :schnaq.last-added/share-hash))
          edit-hash (or (:schnaq.last-added/edit-hash local-storage)
                        (ls/get-item :schnaq.last-added/edit-hash))]
      (when-not (and (nil? edit-hash) (nil? share-hash))
        {:fx [[:dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash]]]}))))

(rf/reg-event-fx
  :initialise-db
  (fn [_ _]
    {:db schnaq-db/default-db
     :fx [[:dispatch [:load/schnaqs]]
          [:dispatch [:username/from-localstorage]]
          [:dispatch [:how-to-visibility/from-localstorage-to-app-db]]
          [:dispatch [:keycloak/init]]
          [:dispatch [:load/last-added-schnaq]]
          [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.save-admin-access/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]]}))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-discussions]]
    (assoc-in db [:schnaqs :all] all-discussions)))

(rf/reg-event-fx
  :form/should-clear
  (fn [_ [_ form-elements]]
    {:fx [[:form/clear form-elements]]}))

(rf/reg-sub
  :current-locale
  (fn [db _]
    (get db :locale)))

(rf/reg-fx
  ;; Changes the HTML lang attribute accordingly.
  :change-document-lang
  (fn [lang-short]
    (let [locale-string (case lang-short
                          :de "de-DE"
                          :en "en-US"
                          "en-US")]
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
       (reitit-frontend/match-by-path navigation/router (str (-> js/window .-location .-origin) "/" url))])))

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