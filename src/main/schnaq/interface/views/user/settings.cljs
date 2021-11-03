(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed-overview]
            [schnaq.interface.views.pages :as pages]))

(defn- current-user []
  (let [user @(rf/subscribe [:user/current])]
    [:div.pl-4
     [common/avatar-with-nickname-right #:user.registered{:profile-picture (get-in user [:profile-picture :display])
                                                          :display-name (get-in user [:names :display])} 40]]))

(defn- settings-button
  "Create a button for the feed list."
  [icon-name label route]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")]
    [:article
     [:a.btn.btn-link.text-left {:class button-class
                                 :role "button"
                                 :href (rfe/href route)}
      [:div.row.text-left
       [:div.col-1
        [icon icon-name "mr-4 my-auto"]]
       [:div.col
        [:span (labels label)]]]]]))

(defn- back-button []
  [tooltip/text
   (labels :history.all-schnaqs/tooltip)
   [:a.button.btn.btn-dark.p-3
    {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaqs/personal])}
    [:div.d-flex
     [icon :arrow-left "m-auto"]]]])

(defn- edit-user-panel []
  [:section
   [back-button]
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

(rf/reg-event-db
  :user.settings.temporary/reset
  (fn [db _] (assoc-in db [:user :settings :temporary] nil)))
