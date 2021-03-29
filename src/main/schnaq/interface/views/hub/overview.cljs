(ns schnaq.interface.views.hub.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
  :hub.schnaqs/add-success
  (fn [{:keys [db]} [_ form response]]
    (let [hub (:hub response)]
      {:db (assoc-in db [:hubs (:hub/keycloak-name hub)] hub)
       :fx [[:dispatch [:notification/add
                        #:notification{:title (labels :hub.add.schnaq.success/title)
                                       :body (labels :hub.add.schnaq.success/body)
                                       :context :success}]]
            [:form/clear form]]})))

(rf/reg-event-fx
  :hub.schnaqs/add-failure
  (fn [_ _]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :hub.add.schnaq.error/title)
                                     :body (labels :hub.add.schnaq.error/body)
                                     :context :danger}]]]}))

(rf/reg-event-fx
  :hub.remove/schnaq
  (fn [{:keys [db]} [_ share-hash]]
    (let [keycloak-name (get-in db [:current-route :path-params :keycloak-name])]
      {:fx [(http/xhrio-request db :delete (gstring/format "/hub/%s/remove" keycloak-name)
                                [:hub.remove.schnaq/success]
                                {:share-hash share-hash}
                                [:hub.remove.schnaq/failure])]})))

(rf/reg-event-fx
  :hub.remove.schnaq/success
  (fn [{:keys [db]} [_ response]]
    (let [hub (:hub response)]
      {:db (assoc-in db [:hubs (:hub/keycloak-name hub)] hub)
       :fx [[:dispatch [:notification/add
                        #:notification{:title (labels :hub.remove.schnaq.success/title)
                                       :body (labels :hub.remove.schnaq.success/body)
                                       :context :success}]]]})))

(rf/reg-event-fx
  :hub.remove.schnaq/failure
  (fn [_ _]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :hub.remove.schnaq.error/title)
                                     :body (labels :hub.remove.schnaq.error/body)
                                     :context :danger}]]]}))

(defn hub-settings
  "Additional hub settings that are displayed in the feed."
  []
  [:div.mx-2
   [:form.pb-3.w-75
    {:on-submit (fn [e]
                  (js-wrap/prevent-default e)
                  (println e)
                  (rf/dispatch [:hub.schnaqs/add (oget e [:target :elements])]))}
    [:label (labels :hub.add.schnaq.input/label)]
    [:input.form-control {:name "schnaq-add-input"
                          :required true
                          :placeholder (labels :hub.add.schnaq.input/placeholder)}]
    [:button.btn.btn-primary.mt-1
     {:type "submit"}
     (labels :hub.add.schnaq.input/button)]]])

(defn- schnaq-entry-with-deletion
  "Displays a single schnaq of the schnaq list for the hub, with the option to delete it from the hub."
  [schnaq]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:article
     {;; The position is needed for the delete button's absolute positioning to work
      :style {:position :relative}}
     [:article.meeting-entry
      {:on-click (fn []
                   (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                 {:share-hash share-hash}])
                   (rf/dispatch [:schnaq/select-current schnaq]))}
      [:div [:img.meeting-entry-title-header-image {:src url}]]
      [:div.px-4.d-flex
       [:div.meeting-entry-title
        [:h5 title]]
       [:div.ml-auto.mt-3
        [badges/read-only-badge schnaq]]]
      [:div.px-4
       [badges/static-info-badges schnaq]]]
     [:button.btn.btn-rounded-2.btn-secondary.schnaq-delete-button
      {:title (labels :hub.remove.schnaq/tooltip)
       :on-click #(when (js/confirm (labels :hub.remove.schnaq/prompt))
                    (rf/dispatch [:hub.remove/schnaq share-hash]))}
      [:i {:class (str "m-auto fas " (fa :cross))}]]]))

(>defn- hub-index
  "Shows the page for an overview of schnaqs for a hub. Takes a keycloak-name which
  uniquely refers to a hub."
  [keycloak-name]
  [string? :ret vector?]
  [pages/three-column-layout
   {:page/heading (gstring/format (labels :hub/heading) keycloak-name)}
   [feed/feed-navigation]
   [feed/schnaq-list-view [:hubs/schnaqs keycloak-name] schnaq-entry-with-deletion]
   [:<>
    [hub-settings]
    [:hr]
    [feed/sidebar-common]]])

(defn hub-overview
  "Renders all schnaqs belonging to the hub."
  []
  (let [keycloak-name (get-in @(rf/subscribe [:navigation/current-route])
                              [:path-params :keycloak-name])]
    [hub-index keycloak-name]))

(rf/reg-event-fx
  :hub/load
  (fn [{:keys [db]} [_ keycloak-name]]
    {:fx [(http/xhrio-request db :get (str "/hub/" keycloak-name) [:hub.load/success keycloak-name])]}))

(rf/reg-event-fx
  :hubs.personal/load
  (fn [{:keys [db]}]
    {:fx [(http/xhrio-request db :get "/hubs/personal" [:hubs.load/success])]}))

(rf/reg-event-db
  :hubs.load/success
  (fn [db [_ {:keys [hubs]}]]
    (when-not (empty? hubs)
      (let [formatted-hubs
            (into {} (map #(vector (:hub/keycloak-name %) %) hubs))]
        (assoc db :hubs formatted-hubs)))))

(rf/reg-event-db
  :hub.load/success
  (fn [db [_ keycloak-name response]]
    (assoc-in db [:hubs keycloak-name] (:hub response))))

(rf/reg-sub
  :hubs/schnaqs
  (fn [db [_ keycloak-name]]
    (get-in db [:hubs keycloak-name :hub/schnaqs] [])))

(rf/reg-sub
  :hubs/all
  (fn [db] (:hubs db)))