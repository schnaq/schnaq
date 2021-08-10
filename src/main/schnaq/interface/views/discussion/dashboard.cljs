(ns schnaq.interface.views.discussion.dashboard
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.views.discussion.common :as dcommon]
            [schnaq.interface.views.discussion.pie-chart :as pie-chart]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.schnaq.summary :as summary]
            [schnaq.interface.views.user :as user]))


(defn- dashboard-statement [statement]
  (let [chart-data (pie-chart/create-vote-chart-data statement)
        path-params (:path-params @(rf/subscribe [:navigation/current-route]))]
    [:div.meeting-entry.my-3.p-3
     {:href "#"
      :on-click (dcommon/navigate-to-statement-on-click statement path-params)}
     [:div.row.h-100
      [:div.col-4
       [user/user-info (:statement/author statement) 24]]
      [:div.col-5
       [:div [md/as-markdown (:statement/content statement)]]]
      [:div.col-3
       [:div.dashboard-pie-chart
        [pie-chart/pie-chart-component chart-data]]]]]))

(defn- schnaq-statistics []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        starting-conclusions (:discussion/starting-statements current-discussion)]
    [:div.panel-white
     [:h3.mb-3 (labels :dashboard/top-posts)]
     (for [statement starting-conclusions]
       [:div {:key (:db/id statement)}
        [dashboard-statement statement]])]))

(defn- beta-only-modal
  "Basic modal which is presented to users trying to access beta features."
  []
  [:div.panel-grey.p-0
   [:div.bg-primary.p-3
    [:div.display-6.text-white (labels :beta.modal/title)]]
   [:div.p-3
    [:text-sm [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " " (labels :beta.modal/explain)]
    [:text-sm (labels :beta.modal/persuade)]
    [:a.btn.btn-primary.mx-auto.d-block
     {:href "mailto:hello@schnaq.com"}
     (labels :beta.modal/cta)]]])

(defn- summary-view []
  (let [beta-user? @(rf/subscribe [:user/beta-tester?])
        current-schnaq @(rf/subscribe [:schnaq/selected])
        title (:discussion/title current-schnaq)]
    [:div.panel-white.p-3
     [:h3.mb-3 (labels :dashboard/summary)]
     [:h5.my-3.text-primary title]
     (if beta-user?
       [summary/summary-body]
       [beta-only-modal])]))

(defn- schnaq-summaries []
  [summary-view])

(defn- count-information [icon number-of unit]
  [:div.panel-white.px-5.mb-3
   [:div.row
    [:div.col-3 [:img.dashboard-info-icon.ml-auto.w-100 {:src (img-path icon)}]]
    [:div.col [:div.display-5 number-of]]]
   [:div.row
    [:div.col.offset-3
     [:text-sm.text-muted (labels unit)]]]])

(defn- schnaq-infos []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:<>
     [count-information :icon-posts statement-count :dashboard/posts]
     [count-information :icon-users user-count :dashboard/members]]))

(defn- dashboard-view []
  [:div.row.m-0
   [:div.col-md-3.p-0.p-md-3
    [schnaq-infos]]
   [:div.col-md-5.col-12.mb-3.p-0.p-md-3
    [schnaq-summaries]]
   [:div.col-md-4.col-12.mb-3.p-0.p-md-3
    [schnaq-statistics]]])

(defn- page-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [dashboard-view]]))

(defn view []
  [page-view])