(ns schnaq.interface.views.user.settings
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [pro-badge
                                                        role-indicator]]
            [schnaq.interface.components.icons :refer [icon icon-with-tooltip]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]
            [schnaq.user :as user :refer [usage-warning-level
                                          warning-level-class]]))

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

(defn- unlimited-icon []
  [icon-with-tooltip (labels :user.settings.features/unlimited) :infinity])

(defn- external-link-icon []
  [icon :external-link-alt "ms-2" {:size "xs"}])

(defn- settings-link [attrs body]
  [:a attrs
   body
   [external-link-icon]])

(defn- feature-available
  "Check feature availability and return an icon for it."
  [feature]
  (let [user @(rf/subscribe [:user/entity])
        disabled? (= false (user/feature-limit user feature))]
    (if disabled?
      [icon :cross "text-danger"]
      [check-icon])))

(defn- feature-overview []
  (let [user @(rf/subscribe [:user/entity])
        {:keys [total-schnaqs]} @(rf/subscribe [:user/meta])]
    [:section.pt-4
     [:dl.row
      [:dt.col-sm-7 (labels :user.settings.features/schnaqs-created)]
      [:dd.col-sm-5 (let [limit (user/feature-limit user :total-schnaqs)
                          warning-class (warning-level-class (usage-warning-level user :total-schnaqs total-schnaqs))]
                      [:span {:class warning-class}
                       total-schnaqs " " (labels :user.settings.features/of) " " (or limit [unlimited-icon])])]

      [:dt.col-sm-7 (labels :user.settings.features/posts-per-schnaq)]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :posts-per-schnaq)]
                      limit [unlimited-icon])]

      [:dt.col-sm-7 (labels :user.settings.features/concurrent-users)]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :concurrent-users)]
                      limit [unlimited-icon])]

      [:dt.col-sm-7 (labels :user.settings.features/mail-notifications)]
      [:dd.col-sm-5
       [settings-link {:href (navigation/href :routes.user.manage/notifications)}
        [check-icon]]]

      [:dt.col-sm-7 (labels :user.settings.features/theming)]
      [:dd.col-sm-5
       [settings-link {:href (navigation/href :routes.user.manage/themes)}
        [feature-available :theming?]]]

      [:dt.col-sm-7 (labels :user.settings.features/embeddings)]
      [:dd.col-sm-5
       [settings-link {:href "https://academy.schnaq.com" :target :_blank}
        [feature-available :embeddings?]]]]

     [:strong (labels :user.settings.features/interactions)]
     [:dl.row
      [:dt.col-sm-7 (labels :user.settings.features/polls)]
      [:dd.col-sm-5 (if-let [limit (user/feature-limit user :polls)]
                      limit [unlimited-icon])]

      [:dt.col-sm-7 (labels :user.settings.features/rankings)]
      [:dd.col-sm-5 [feature-available :rankings?]]

      [:dt.col-sm-7 (labels :user.settings.features/wordclouds)]
      [:dd.col-sm-5 [feature-available :wordcloud?]]]]))

(defn- outline-info-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn- feature-and-coc-buttons []
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
   (when @(rf/subscribe [:user/authenticated?])
     [:<>
      [:a.text-decoration-none {:href (navigation/href :routes.user.manage/account)}
       [:div.d-flex.d-row
        [common/avatar-with-nickname-right 40]
        [:div.align-self-center [role-indicator true]]]]
      [feature-overview]
      [:hr.my-4]])
   [feature-and-coc-buttons]])

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
