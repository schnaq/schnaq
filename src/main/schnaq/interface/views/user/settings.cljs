(ns schnaq.interface.views.user.settings
  (:require [goog.string :refer [format]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [pro-badge info-icon-with-tooltip]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]
            [schnaq.user :as user]))

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

(defn- check-icon []
  [icon :check/circle "text-success"])

(defn- feature-available
  "Check feature availability and return an icon for it."
  [feature]
  (let [user @(rf/subscribe [:user/entity])
        disabled? (= false (user/feature-limit user feature))]
    (if disabled?
      [icon :cross "text-danger"]
      [check-icon])))

(defn- feature-overview []
  (let [user @(rf/subscribe [:user/entity])]
    [:<>
     [:strong.mt-3.d-block "Featureübersicht"]
     [:dl.row
      [:dt.col-sm-7 "schnaqs erstellt"]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :total-schnaqs)]
                      (format "%d %s %d" :todo "von" limit)
                      (format "%d" :todo))]

      [:dt.col-sm-7 "Beiträge pro schnaq"]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :posts-per-schnaq)]
                      limit "unbegrenzt")]

      [:dt.col-sm-7 "Max. gleichzeitige User pro schnaq"]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :concurrent-users)]
                      limit "unbegrenzt")]

      [:dt.col-sm-7 "Persönliches Design"]
      [:dd.col-sm-5 [feature-available :theming?]]

      [:dt.col-sm-7 "E-Mail Benachrichtigungen"]
      [:dd.col-sm-5 [check-icon]]

      [:dt.col-sm-7 "Integrationen?"
       [:a {:href "https://academy.schnaq.com" :target :_blank}
        [info-icon-with-tooltip "Lerne mehr in der schnaq academy."]]]
      [:dd.col-sm-5 [feature-available :embeddings?]]]

     [:strong "Interaktionsfunktionen"]
     [:dl.row
      [:dt.col-sm-7 "Umfragen"]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :polls)]
                      limit "unbegrenzt")]

      [:dt.col-sm-7 "Rankings"]
      [:dd.col-sm-5 [feature-available :rankings?]]

      [:dt.col-sm-7 "Wortwolke"]
      [:dd.col-sm-5 [feature-available :wordcloud?]]]]))

(defn- outline-info-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn- feature-and-coc-butons []
  (let [pro-user? @(rf/subscribe [:user/pro?])]
    [:section.panel-white.text-center
     [:div.btn-group {:role "group"}
      [:div.btn-group-vertical
       [outline-info-button :user/features
        (navigation/href (if pro-user? :routes.welcome/pro :routes.welcome/free))]
       [outline-info-button :coc/heading "https://schnaq.com/code-of-conduct"]]]]))

(defn user-info-box
  "Display an overview of a user's features."
  []
  [:section.panel-white
   [common/avatar-with-nickname-right 40]
   [feature-overview]
   [:hr.my-4]
   [feature-and-coc-butons]])

(defn user-view [page-heading-label content]
  [pages/three-column-layout
   {:page/heading (labels page-heading-label)
    :condition/needs-authentication? true}
   [edit-user-panel]
   content
   [user-info-box]])

(rf/reg-event-db
 :user.settings.temporary/reset
 (fn [db _] (update-in db [:user :settings] dissoc :temporary)))
