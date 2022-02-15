(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed-overview]
            [schnaq.interface.views.pages :as pages]))

(defn- current-user []
  (let [user @(rf/subscribe [:user/current])]
    [:div.ps-4
     [common/avatar-with-nickname-right #:user.registered{:profile-picture (get-in user [:profile-picture :display])
                                                          :display-name (get-in user [:names :display])} 40]]))

(defn- settings-button
  "Create a button for the feed list."
  [icon-name label route]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")]
    [:article
     [:a.btn.btn-link.text-start {:class button-class
                                  :role "button"
                                  :href (navigation/href route)}
      [:div.row.text-start
       [:div.col-1
        [icon icon-name "me-4 my-auto"]]
       [:div.col
        [:span (labels label)]]]]]))

(defn- back-button []
  [tooltip/text
   (labels :history.all-schnaqs/tooltip)
   [:a.button.btn.btn-dark.p-3
    {:href (toolbelt/current-overview-link)}
    [:div.d-flex
     [icon :arrow-left "m-auto"]]]])

(defn- edit-user-panel []
  [:section
   [back-button]
   [:hr.my-4]
   [settings-button :edit :user.settings/info :routes.user.manage/account]
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
