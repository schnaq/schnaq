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

(defn- edit-user-navigation-button []
  [:article
   [:a.btn.feed-button-focused
    {:href (rfe/href :routes.user.manage/account)}
    [:div.d-flex.flex-row.text-left
     [:i.mr-4.my-auto {:class (str "fas " (fa :user/edit))}]
     [:span (labels :user.settings/info)]]]])

(defn- edit-user-panel []
  [:section
   [:h6.text-muted.mb-4 (labels :user.settings)]
   [:hr.my-4]
   [edit-user-navigation-button]])

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
