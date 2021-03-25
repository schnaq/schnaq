(ns schnaq.interface.views.user.elements
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.views.user :as user]))

(defn about-button [label href-link]
  [:div.btn-block
   [:a.btn.btn-outline-primary.rounded-2.w-100 {:href href-link}
    (labels label)]])

(defn extra-information []
  [:div.feed-extra-info.text-right
   [:div.btn-group-vertical
    [about-button :router/privacy (reitfe/href :routes/privacy)]
    [about-button :coc/heading (reitfe/href :routes/code-of-conduct)]
    [about-button :how-to/button (reitfe/href :routes/how-to)]]])

(defn- current-user [user-name]
  [:<>
   [:h6 (labels :user.settings)]
   [user/user-info user-name 32]])

(defn- user-navigation []
  [:<>
   [:h6 (labels :user.settings)]])

(defn- user-panel []
  [:div
   [current-user]
   [:hr]
   [user-navigation]])

(defn user-view-desktop [_user content]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-3.py-4.px-5
     [user-panel]]
    [:div.col-6.py-4
     content]
    [:div.col-3.py-4
     [extra-information]]]])