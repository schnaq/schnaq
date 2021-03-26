(ns schnaq.interface.views.hub.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
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

(defn hub-settings
  "Additional hub settings that are displayed in the feed."
  []
  [:div.mx-2
   [:label (labels :hub.add.schnaq.input/label)]
   [:form.pb-3
    {:on-submit (fn [e]
                  (js-wrap/prevent-default e)
                  (println e)
                  (rf/dispatch [:hub.schnaqs/add (oget e [:target :elements])]))}
    [:div.form-row
     [:div.col
      [:input.form-control {:name "schnaq-add-input"
                            :required true
                            :placeholder (labels :hub.add.schnaq.input/placeholder)}]]
     [:div.col
      [:button.btn.btn-secondary
       {:type "submit"}
       (labels :hub.add.schnaq.input/button)]]]]])

(>defn- hub-index
  "Shows the page for an overview of schnaqs for a hub. Takes a keycloak-name which
  uniquely refers to a hub."
  [keycloak-name]
  [string? :ret vector?]
  [pages/three-column-layout
   {:page/heading (gstring/format (labels :hub/heading) keycloak-name)}
   [feed/feed-navigation]
   [:<>
    [hub-settings]
    [feed/schnaq-list-view [:hubs/schnaqs keycloak-name]]]
   [feed/feed-extra-information]])

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