(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [pro-badge]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feed.overview :as feed-overview]
            [schnaq.interface.views.pages :as pages]))

(defn- settings-button
  "Create a button for the feed list."
  [icon-name text route]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")]
    [:article
     [:a.btn.btn-link.text-start {:class button-class
                                  :role "button"
                                  :href (navigation/href route)}
      [:div.row.text-start
       [:div.col-1
        [icon icon-name "me-4 my-auto"]]
       [:div.col text]]]]))

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
   [settings-button :edit (labels :user.settings/info) :routes.user.manage/account]
   [settings-button :bell (labels :user.settings/notifications) :routes.user.manage/notifications]
   [settings-button :palette [:<> (labels :user.settings/themes) " " [pro-badge]] :routes.user.manage/themes]])

(defn- feature-overview []
  (let [user @(rf/subscribe [:user/entity])]
    [:<>
     [:h5.pt-3 "Meine Features"]
     [:dl.row
      [:dt.col-sm-7 "schnaqs erstellt"]
      [:dd.col-sm-5 "9 von 10"]

      [:dt.col-sm-7 "Beiträge pro schnaq"]
      [:dd.col-sm-5 "1000"]

      [:dt.col-sm-7 "Max. gleichzeitige User pro schnaq"]
      [:dd.col-sm-5 "120"]

      [:dt.col-sm-7 "Persönliches Design"]
      [:dd.col-sm-5 "nein"]

      [:dt.col-sm-7 [:a {:href "https://academy.schnaq.com" :target :_blank} "Integrationen?"]]
      [:dd.col-sm-5 "nein"]]
     [:h5 "Interaktionsfunktionen"]
     [:dl.row
      [:dt.col-sm-7 "Wortwolke"]
      [:dd.col-sm-5 "nein"]

      [:dt.col-sm-7 "Umfragen"]
      [:dd.col-sm-5 "0"]

      [:dt.col-sm-7 "Rankings"]
      [:dd.col-sm-5 "nein"]]]))

(defn user-view [page-heading-label content]
  [pages/three-column-layout
   {:page/heading (labels page-heading-label)
    :condition/needs-authentication? true}
   [edit-user-panel]
   content
   [:section.panel-white
    [common/avatar-with-nickname-right 40]
    [feature-overview]
    [:hr.my-4]
    [feed-overview/sidebar-info-links]]])

(rf/reg-event-db
 :user.settings.temporary/reset
 (fn [db _] (update-in db [:user :settings] dissoc :temporary)))
