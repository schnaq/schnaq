(ns schnaq.interface.views.user.edit-hubs
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.user.settings :as settings]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]))

(defn- show-hubs []
  (let [hubs @(rf/subscribe [:hubs/all])]
    [pages/settings-panel
     (labels :user.settings.hubs/show)
     (if (empty? hubs)
       [:h4.text-muted (labels :user.settings.hubs/empty)]
       [hub/hub-list hubs])]))

(defn view []
  [settings/user-view :user/edit-hubs [show-hubs]])
