(ns schnaq.interface.events
  (:require [ajax.core :as ajax]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reitit.frontend :as reitit-frontend]
            [schnaq.interface.db :as schnaq-db]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.views.modals.modal :as modal]))

(rf/reg-event-fx
  :username/from-localstorage
  (fn [{:keys [db]} _]
    (if-let [name (ls/get-item :username)]
      {:db (assoc-in db [:user :name] name)}
      {:fx [[:dispatch [:username/notification-set-name]]]})))

(rf/reg-event-fx
  :username/notification-set-name
  (fn [_ _]
    (let [notification-id "username/notification-set-name"]
      {:fx [[:dispatch
             [:notification/add
              #:notification{:id notification-id
                             :title (labels :user.set-name/dialog-header)
                             :body [:<>
                                    [:p (labels :user.set-name/dialog-lead)]
                                    [:p (labels :user.set-name/dialog-body)]
                                    [:div.mt-2.btn.btn-sm.btn-outline-primary
                                     {:on-click #(rf/dispatch [:username/open-dialog])}
                                     (labels :user.set-name/dialog-button)]]
                             :context :info
                             :stay-visible? true}]]]})))

(rf/reg-event-fx
  :username/open-dialog
  (fn [{:keys [db]} _]
    (let [username (get-in db [:user :name])]
      (when (or (= "Anonymous" username)
                (nil? username))
        {:fx [[:dispatch [:user/set-display-name "Anonymous"]]
              [:dispatch [:modal {:show? true
                                  :child [modal/enter-name-modal]}]]]}))))

(rf/reg-event-fx
  :load/schnaqs
  (fn [_ _]
    (when-not toolbelt/production?
      {:fx [[:http-xhrio {:method :get
                          :uri (str (:rest-backend config) "/schnaqs")
                          :timeout 10000
                          :response-format (ajax/transit-response-format)
                          :on-success [:init-from-backend]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :load/last-added-schnaq
  (fn [_ _]
    (let [share-hash (ls/get-item :schnaq.last-added/share-hash)
          edit-hash (ls/get-item :schnaq.last-added/edit-hash)]
      (when-not (and (nil? edit-hash) (nil? share-hash))
        {:fx [[:dispatch [:schnaq/load-by-hash-as-admin share-hash edit-hash]]]}))))

(rf/reg-event-fx
  :initialise-db
  (fn [_ _]
    {:db schnaq-db/default-db
     :fx [[:dispatch [:load/schnaqs]]
          [:dispatch [:username/from-localstorage]]
          [:dispatch [:keycloak/init]]
          [:dispatch [:load/last-added-schnaq]]
          [:dispatch [:visited.save-statement-nums/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.save-admin-access/store-hashes-from-localstorage]]
          [:dispatch [:schnaqs.visited/store-hashes-from-localstorage]]]}))

(rf/reg-event-db
  :init-from-backend
  (fn [db [_ all-discussions]]
    (assoc-in db [:schnaqs :all] all-discussions)))

(rf/reg-event-db
  :admin/set-password
  (fn [db [_ password]]
    (assoc-in db [:admin :password] password)))

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

(rf/reg-event-fx
  :language/set-and-redirect
  (fn [_ [_ locale redirect-url]]
    {:fx [[:dispatch [:set-locale locale]]
          [:change-location redirect-url]]}))