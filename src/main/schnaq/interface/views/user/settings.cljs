(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed-overview]
            [schnaq.interface.views.pages :as pages]))

(defn about-button [label href-link]
  [:div.btn-block
   [:a.btn.btn-outline-primary.rounded-2.w-100 {:href href-link}
    (labels label)]])

(defn- current-user []
  (let [user-name @(rf/subscribe [:user/display-name])]
    [:<>
     [:div.pl-4
      [common/avatar-with-nickname-right user-name 40]]]))

(defn- edit-user-navigation-button [label icon route]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= route current-route) "feed-button-focused"
                                                 "feed-button")]
    [:article
     [:button
      {:class button-class :type "button"
       :on-click #(rf/dispatch [:navigation/navigate route])}
      [:div.d-flex.flex-row.text-left
       [:i.mr-4.my-auto {:class (str "fas " (fa icon))}]
       [:span (labels label)]]]]))

(defn- user-navigation []
  [:<>
   [edit-user-navigation-button :user.settings/info :user/edit :routes.user.manage/account]])

(defn- edit-user-panel []
  [:section
   [:h6.text-muted.mb-4 (labels :user.settings)]
   [:hr.my-4]
   [user-navigation]])

(defn user-view [page-heading-label content]
  [pages/three-column-layout
   {:page/heading (labels page-heading-label)}
   [edit-user-panel]
   content
   [:section.panel-white
    [current-user]
    [:hr.my-4]
    [feed-overview/sidebar-common]]])
