(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed-overview]
            [schnaq.interface.views.pages :as pages]))

(defn about-button [label href-link]
  [:div.btn-block
   [:a.btn.btn-outline-primary.rounded-2.w-100 {:href href-link}
    (labels label)]])

(defn- current-user []
  (let [user @(rf/subscribe [:user/current])]
    [:div.pl-4
     [common/avatar-with-nickname-right #:user.registered{:profile-picture (get-in user [:profile-picture :display])
                                                          :display-name (get-in user [:names :display])} 40]]))

(defn- settings-button
  "Create a button for the feed list."
  [icon label route]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")]
    [:article
     [:a.btn.btn-link.text-left {:class button-class
                                 :role "button"
                                 :href (rfe/href route)}
      [:div.row.text-left
       [:div.col-1
        [:i.mr-4.my-auto {:class (str "fas " (fa icon))}]]
       [:div.col
        [:span (labels label)]]]]]))

(defn- edit-user-panel []
  [:section
   [:h6.text-muted.mb-4 (labels :user.notification)]
   [:hr.my-4]
   [settings-button :user/edit :user.settings/info :routes.user.manage/account]
   [settings-button :bell :user.settings/notifications :routes.user.manage/notifications]])

(defn user-view [page-heading-label content]
  [pages/three-column-layout
   {:page/heading (labels page-heading-label)
    :condition/needs-authentication? true}
   [edit-user-panel]
   content
   [:section.panel-white
    [current-user]
    [:hr.my-4]
    [feed-overview/sidebar-info-links]]])
