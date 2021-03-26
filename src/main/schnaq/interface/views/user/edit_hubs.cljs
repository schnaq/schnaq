(ns schnaq.interface.views.user.edit-hubs
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.elements :as elements]
            [schnaq.interface.views.hub.common :as hub]))

(defn- show-hubs []
  (let [hubs @(rf/subscribe [:hubs/all])]
    [:div.manage-account-content.shadow-straight-light
     [:h4.text-gray-600.mb-5 (labels :user.settings.hubs/show)]
     (if (empty? hubs)
       [:h4.text-gray-600 (labels :user.settings.hubs/empty)]
       [hub/hub-list hubs])]))

(defn- content []
  [pages/with-nav
   {:page/heading (labels :user/edit-hubs)}
   [elements/user-view
    [show-hubs]]])

(defn view []
  [content])


