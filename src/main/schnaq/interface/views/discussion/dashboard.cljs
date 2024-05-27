(ns schnaq.interface.views.discussion.dashboard
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.wordcloud :refer [wordcloud]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.views.discussion.pie-chart :as pie-chart]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user :as user]))

(defn- dashboard-statement [statement-id]
  (let [statement @(rf/subscribe [:schnaq/statement statement-id])
        path-params (:path-params @(rf/subscribe [:navigation/current-route]))]
    [:div.schnaq-entry.my-3.p-3
     [:a.link-unstyled
      {:href (navigation/href :routes.schnaq.select/statement (assoc path-params :statement-id statement-id))}
      [:div.row.h-100
       [:div.col-xl-4.col-12
        [user/user-info statement 24]]
       [:div.col-xl-5.col-7
        [md/as-markdown (:statement/content statement)]]
       [:div.col-xl-3.col-5
        [:div.dashboard-pie-chart
         (when statement
           [pie-chart/create-vote-chart-data statement])]]]]]))

(defn- schnaq-statistics []
  (let [starting-conclusion-ids @(rf/subscribe [:schnaq.statements/current-level])]
    [:div.panel-white
     [:h3.mb-3 (labels :dashboard/top-posts)]
     (for [statement-id starting-conclusion-ids]
       (with-meta [dashboard-statement statement-id]
         {:key (str "dashboard-statement-" statement-id)}))]))

(defn- count-information [icon icon-alt-text number-of unit]
  [:div.panel-white.px-5.mb-3
   [:div.row
    [:div.col-5.col-xl-3.align-self-center [:img.dashboard-info-icon.ms-auto.w-100 {:src (img-path icon)
                                                                                    :alt (labels icon-alt-text)}]]
    [:div.col [:div.display-5 number-of]]]
   [:div.row
    [:div.col.offset-xl-3.offset-5
     [:text-sm.text-muted (labels unit)]]]])

(defn- schnaq-infos []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:<>
     [count-information :icon-posts :icon.posts/alt-text statement-count :dashboard/posts]
     [count-information :icon-users :icon.users/alt-text user-count :dashboard/members]]))

(defn- wordcloud-view
  "Display a word cloud with common words of the discussion."
  []
  [:section.panel-white.mb-3
   [:h3 (labels :dashboard.wordcloud/title)]
   [:small.text-muted (labels :dashboard.wordcloud/subtitle)]
   [wordcloud @(rf/subscribe [:wordcloud/words])]])

(defn- dashboard-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.row.m-0
      [:div.col-lg-3.p-0.p-md-3
       [schnaq-infos]]
      [:div.col-lg-5.col-12.mb-3.p-0.p-md-3
       [wordcloud-view]]
      [:div.col-lg-4.col-12.mb-3.p-0.p-md-3
       [schnaq-statistics]]]]))

(defn view []
  [dashboard-view])
