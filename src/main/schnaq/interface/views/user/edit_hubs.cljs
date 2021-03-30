(ns schnaq.interface.views.user.edit-hubs
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.user.settings :as settings]
            [schnaq.interface.views.hub.common :as hub]))

(defn- show-hubs []
  (let [hubs @(rf/subscribe [:hubs/all])]
    [:div.panel-white.p-5
     [:h4.text-gray-600.mb-5 (labels :user.settings.hubs/show)]
     (if (empty? hubs)
       [:h4.text-gray-600 (labels :user.settings.hubs/empty)]
       [hub/hub-list hubs])]))

(defn view []
  [settings/user-view :user/edit-hubs [show-hubs]])


