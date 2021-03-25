(ns schnaq.interface.views.user.edit-hubs
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.elements :as elements]))

(defn- show-hubs [user-groups]
  [:div.manage-account-content.shadow-straight-light
   [:h4.text-gray-600.mb-5 (labels :user.hubs/show)]
   (if (empty? user-groups)
     [:h4.text-gray-600 (labels :user.hubs/empty)]
     (map-indexed (fn [i group]
                    [:div.pb-4 {:key (str "group-" i "-" group)}
                     [:h5 group]]) user-groups))])

(defn- content []
  (let [user @(rf/subscribe [:user/data])
        user-groups (get-in user [:groups])]
    [pages/with-nav
     {:page/heading (labels :user/edit-hubs)}
     [elements/user-view user
      [show-hubs user-groups]]]))

(defn view []
  [content])