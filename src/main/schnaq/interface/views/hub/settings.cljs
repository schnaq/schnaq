(ns schnaq.interface.views.hub.settings
  (:require [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.feed.overview :as feed]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.hub.overview :as hubs]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.utils.http :as http]))

(defn- settings-body []
  (let [{:hub/keys [name]} @(rf/subscribe [:hub/current])
        input-id :change-hub-name-input]
    [pages/settings-panel
     (labels :hub.settings/change-name)
     [:form
      {:on-submit (fn [e]
                    (let [new-hub-name (oget+ e [:target :elements input-id :value])]
                      (js-wrap/prevent-default e)
                      (rf/dispatch [:hub.name/update new-hub-name])))}
      [:div.d-flex.flex-row
       [:div.mr-4 [common/avatar name 50]]
       [common/form-input {:id input-id
                           :default-value name
                           :css "font-150"}]]
      [:div.text-right.my-3
       [:button.btn.btn-lg.btn-outline-primary.rounded-2 {:type :submit}
        (labels :user.settings.button/change-account-information)]]]]))

(>defn- settings-view
  "Show the CRUD view for a hub."
  []
  [:ret vector?]
  (let [keycloak-name (get-in @(rf/subscribe [:navigation/current-route])
                              [:path-params :keycloak-name])]
    [pages/three-column-layout
     {:page/heading (gstring/format (labels :hub/heading) keycloak-name)}
     [feed/feed-navigation]
     [settings-body]
     [hubs/sidebar-right]]))

(defn settings
  "Renders all schnaqs belonging to the hub."
  []
  [settings-view])


(rf/reg-event-fx
  :hub.name/update
  (fn [{:keys [db]} [_ new-hub-name]]
    {:fx [(http/xhrio-request db :put "/hub/name"
                              [:hub/load]
                              {:hub-name new-hub-name}
                              [:ajax.error/as-notification])]}))
