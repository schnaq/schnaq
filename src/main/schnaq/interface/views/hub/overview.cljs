(ns schnaq.interface.views.hub.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [schnaq.interface.config :as config]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]))

(>defn- hub-index
  "Shows the page for an overview of schnaqs for a hub. Takes a keycloak-name which
  uniquely refers to a hub."
  [keycloak-name]
  [string? :ret vector?]
  [pages/with-nav
   {:page/heading (gstring/format (labels :hub/heading) keycloak-name)}
   [:div.container-fluid.px-0
    [toolbelt/desktop-mobile-switch
     [feed/feed-page-desktop [:hubs/schnaqs keycloak-name]]
     [feed/feed-page-mobile [:hubs/schnaqs keycloak-name]]]]])

(defn hub-overview
  "Renders all schnaqs belonging to the hub."
  []
  (let [keycloak-name (get-in @(rf/subscribe [:navigation/current-route])
                              [:path-params :keycloak-name])]
    [hub-index keycloak-name]))

(rf/reg-event-fx
  :hub/load
  (fn [{:keys [db]} [_ keycloak-name]]
    {:fx [[:http-xhrio {:method :get
                        :uri (str (:rest-backend config/config) "/hub/" keycloak-name)
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
    (get-in db [:hubs keycloak-name :hub/schnaqs] [])))