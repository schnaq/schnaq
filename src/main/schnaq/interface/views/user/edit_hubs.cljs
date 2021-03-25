(ns schnaq.interface.views.user.edit-hubs
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.user.elements :as elements]
            [schnaq.interface.views.pages :as pages]))

(defn- change-hubs []
  [:div "hubs"])

(defn- content []
  (let [user @(rf/subscribe [:user/data])]
    [pages/with-nav
     {:page/heading (labels :user/edit-hubs)}
     [elements/user-view-desktop user
      [change-hubs]]]))

(defn view []
  [content])