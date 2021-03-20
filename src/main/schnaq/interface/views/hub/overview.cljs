(ns schnaq.interface.views.hub.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [ajax.core :as ajax]
            [schnaq.interface.config :as config]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]))

(defn- hub-schnaq-list-view
  "Shows a list of schnaqs."
  [keycloak-name]
  [:div.meetings-list
   (let [schnaqs @(rf/subscribe [:hubs/schnaqs keycloak-name])]
     (if (empty? schnaqs)
       [feed/no-schnaqs-found]
       (for [schnaq schnaqs]
         [:div.pb-4 {:key (:db/id schnaq)}
          [feed/schnaq-entry schnaq]])))])

(defn- feed-page-desktop [keycloak-name]
  [:div.row.px-0.mx-0.py-3
   [:div.col-3.py-3
    [feed/feed-navigation]]
   [:div.col-6.py-3.px-5
    [hub-schnaq-list-view keycloak-name]]
   [:div.col-3.py-3
    [feed/feed-extra-information]]])

(defn- feed-page-mobile [keycloak-name]
  [:div.my-3
   [hub-schnaq-list-view keycloak-name]])

(>defn- hub-index
  "Shows the page for an overview of schnaqs for a hub. Takes a keycloak-name which
  uniquely refers to a hub."
  [keycloak-name]
  [string? :ret vector?]
  [pages/with-nav
   {:page/heading "Platzhalter"
    :page/subheading "Platzhalter"}
   [:div.container-fluid.px-0
    [toolbelt/desktop-mobile-switch
     [feed-page-desktop keycloak-name]
     [feed-page-mobile keycloak-name]]]])

(defn hub-overview
  "Renders all schnaqs belonging to the hub."
  []
  [hub-index :keycloak-name])

(rf/reg-event-fx
  :hub/load
  (fn [{:keys [db]} [_ keycloak-name]]
    {:fx [[:http-xhrio {:method :get
                        :uri (str (:rest-backend config/config) "/hubs/" keycloak-name)
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :headers (auth/authentication-header db)
                        :on-success [:hubs.load/success keycloak-name]
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-db
  :hubs.load/success
  (fn [db [_ keycloak-name response]]
    (assoc-in db [:hubs keycloak-name] (:hubs response))))

(rf/reg-sub
  :hubs/schnaqs
  (fn [db [_ keycloak-name]]
    (get-in db [:hubs keycloak-name] [])))