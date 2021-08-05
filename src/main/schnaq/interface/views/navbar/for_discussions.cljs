(ns schnaq.interface.views.navbar.for-discussions
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn clickable-title []
  (let [{:discussion/keys [title share-hash] :as schnaq} @(rf/subscribe [:schnaq/selected])]
    [:<>
     [:small.text-primary (labels :discussion.navbar/title)]
     [:div.clickable-no-hover {:on-click
                               (fn []
                                 (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                               {:share-hash share-hash}])
                                 (rf/dispatch [:schnaq/select-current schnaq]))}
      [:h1.h5.font-weight-bold (toolbelt/truncate-to-n-chars title 30)]]]))

(defn navbar []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-0.mb-md-4
     ;; schnaq logo
     [:a.schnaq-logo-container.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
      [:img.d-inline-block.align-middle.mr-2
       {:src (img-path :logo-white) :alt "schnaq logo"
        :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]
     [:div.mx-4
      [clickable-title]]
     [:div.mx-4.ml-auto.d-none.d-md-block
      [:small.text-primary (labels :discussion.navbar/posts)]
      [:h5.text-center statement-count]]
     [:div.mx-4.d-none.d-md-block
      [:small.text-primary (labels :discussion.navbar/members)]
      [:h5.text-center user-count]]]))

(defn graph-button
  "Rounded square button to navigate to the graph view"
  [share-hash]
  [:button.btn.btn-sm.btn-dark.shadow-sm.mx-auto.rounded-1.h-100
   {:on-click #(rf/dispatch
                 [:navigation/navigate :routes/graph-view
                  {:share-hash share-hash}])}
   [:img.img-fluid
    {:src (img-path :icon-graph)
     :width "25"
     :alt "graph icon"}]
   [:p.small.m-0 (labels :graph.button/text)]])

(defn summary-button
  "Button to navigate to the summary view."
  [share-hash]
  [:button.btn.btn-sm.btn-dark.shadow-sm.mx-auto.rounded-1.h-100
   {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/dashboard {:share-hash share-hash}])}
   [:img.img-fluid
    {:src (img-path :icon-summary)
     :width "25"
     :alt "summary icon"}]
   [:p.small.m-0 (labels :summary.link.button/text)]])

(defn navbar-statements []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.ml-auto.flex-wrap
     [:div.d-flex.align-items-center.schnaq-navbar.px-4.ml-auto
      [:div.mx-2
       [admin/share-link]]
      [admin/txt-export share-hash title]
      (when edit-hash
        [admin/admin-center share-hash edit-hash])]
     [:div.d-flex.align-items-center
      [:div.h-100.mx-2 [graph-button share-hash]]
      [:div.h-100.mx-2 [summary-button share-hash]]
      [:div.d-flex.align-items-center.schnaq-navbar
       [um/user-handling-menu "btn-outline-primary"]]]]))

(defn header
  "Overview header for a discussion."
  []
  [:div.d-flex.flex-row.flex-wrap.p-md-3
   [navbar]
   [navbar-statements]])