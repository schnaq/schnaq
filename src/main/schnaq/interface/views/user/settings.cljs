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
                                  :aria-current (when (= current-route route) "page")
                                  :href (navigation/href route)}
      [:div.row.text-start
       [:div.col-1
        [icon icon-name "me-4 my-auto"]]
       [:div.col text]]]]))

(defn- back-button []
  [tooltip/text
   (labels :history.all-schnaqs/tooltip)
   [:a.button.btn.btn-dark.p-3
    {:href (toolbelt/current-overview-link)
     :aria-label (labels :history.all-schnaqs/tooltip)}
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

(defn- cross-icon []
  [icon :cross "text-danger"])

(defn- unlimited-icon []
  [icon-with-tooltip (labels :user.settings.features/unlimited) :infinity])

(defn- external-link-icon []
  [icon :external-link-alt "ms-2" {:size "xs"}])

(defn- settings-link
  "Link to a feature's settings. The label names the link for assistive technology,
  because its content consists of icons only."
  [label attrs body]
  [:a (assoc attrs :aria-label label)
   body
   [external-link-icon]])

(defn- feature-available
  "Check feature availability and return an icon for it."
  [feature]
  (let [user @(rf/subscribe [:user/entity])
        disabled? (= false (user/feature-limit user feature))]
    (if disabled? [cross-icon] [check-icon])))

(defn- feature-row
  "A feature's label with its value, as one row of the overview."
  [label value]
  [:<>
   [:dt.col-7 label]
   [:dd.col-5 value]])

(defn- limit-or-unlimited
  "A feature's limit or, if it has none, the unlimited icon."
  [user feature]
  (or (user/feature-limit user feature) [unlimited-icon]))

(defn- feature-overview []
  (let [user @(rf/subscribe [:user/entity])
        {:keys [total-schnaqs]} @(rf/subscribe [:user/meta])]
    [:section.pt-4
     [:dl.row
      [feature-row (labels :user.settings.features/schnaqs-created)
       [:span {:class (warning-level-class (usage-warning-level user :total-schnaqs total-schnaqs))}
        total-schnaqs " " (labels :user.settings.features/of) " " (limit-or-unlimited user :total-schnaqs)]]
      [feature-row (labels :user.settings.features/posts-per-schnaq)
       (limit-or-unlimited user :posts-per-schnaq)]
      [feature-row (labels :user.settings.features/concurrent-users)
       (limit-or-unlimited user :concurrent-users)]
      [feature-row (labels :user.settings.features/pro) [check-icon]]
      (let [label (labels :user.settings.features/mail-notifications)]
        [feature-row label
         [settings-link label
          {:href (navigation/href :routes.user.manage/notifications)}
          [check-icon]]])
      (let [label (labels :user.settings.features/theming)]
        [feature-row label
         [settings-link label
          {:href (navigation/href :routes.user.manage/themes)}
          [feature-available :theming?]]])
      (let [label (labels :user.settings.features/embeddings)]
        [feature-row label
         [settings-link label
          {:href "https://academy.schnaq.com" :target :_blank}
          [feature-available :embeddings?]]])]

     [:strong (labels :user.settings.features/interactions)]
     [:dl.row
      [feature-row (labels :user.settings.features/polls)
       (limit-or-unlimited user :polls)]
      [feature-row (labels :user.settings.features/rankings) [feature-available :rankings?]]
      [feature-row (labels :user.settings.features/wordclouds) [feature-available :wordcloud?]]]]))

(defn- features-button []
  [:section.text-center
   [:a.feed-button-outlined {:href (navigation/href :routes.welcome)}
    (labels :user/features)]])

(defn user-info-box
  "Display an overview of a user's features."
  []
  [:section.panel-white.p-3
   (when @(rf/subscribe [:user/authenticated?])
     [:<>
      [:a.text-decoration-none {:href (navigation/href :routes.user.manage/account)}
       [:div.d-flex.d-row
        [common/avatar-with-nickname-right 40]
        [:div.align-self-center [role-indicator]]]]
      [feature-overview]
      [:hr.mt-4.mb-3]])
   [features-button]])

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
