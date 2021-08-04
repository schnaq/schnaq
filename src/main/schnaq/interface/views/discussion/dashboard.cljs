(ns schnaq.interface.views.discussion.dashboard
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.pages :as pages]))

(defn- beta-only-modal
  "Basic modal which is presented to users trying to access beta features."
  []
  [modal/modal-template
   (labels :beta.modal/title)
   [:<>
    [:p [:i {:class (str "m-auto fas fa-lg " (fa :shield))}] " " (labels :beta.modal/explain)]
    [:p (labels :beta.modal/persuade)]
    [:a.btn.btn-primary.mx-auto.d-block
     {:href "mailto:hello@schnaq.com"}
     (labels :beta.modal/cta)]]])

(defn- summary-view []
  (let [beta-user? @(rf/subscribe [:user/beta-tester?])]
    (if beta-user?
      [:div "summary"]
      [beta-only-modal])))

(defn- schnaq-statistics []
  [:div.panel-white])

(defn- schnaq-summaries []
  [:div.panel-white])

(defn- count-information [icon number-of unit]
  [:div.panel-white.px-5.mb-3
   [:div.row
    [:div.col-3 [:img.dashboard-info-icon.ml-auto.w-100 {:src (img-path icon)}]]
    [:div.col [:h4 number-of]]]
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
   [:div.col-3
    [schnaq-infos]]
   [:div.col-5
    [schnaq-summaries]]
   [:div.col-4
    [schnaq-statistics]]])

(defn- page-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [dashboard-view]]))

(defn view []
  [page-view])