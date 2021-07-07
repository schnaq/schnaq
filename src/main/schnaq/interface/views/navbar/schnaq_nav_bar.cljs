(ns schnaq.interface.views.navbar.schnaq-nav-bar
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.config.shared :as shared-conf]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn clickable-title [{:discussion/keys [title share-hash] :as discussion}]
  [:<>
   [:small.text-primary (labels :discussion.title)]
   [:div.clickable-no-hover {:on-click
                             (fn []
                               (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                             {:share-hash share-hash}])
                               (rf/dispatch [:schnaq/select-current discussion]))}
    [:h5 (toolbelt/truncate-to-n-chars title 30)]]])

(defn navbar-schnaq []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4
     ;; schnaq logo
     [:a.schnaq-logo-container {:href (reitfe/href :routes.schnaqs/personal)}
      [:img.d-inline-block.align-middle.mr-2
       {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
     [:div.mx-4
      [clickable-title discussion]]
     [:div.mx-4.ml-auto
      [:small.text-primary (labels :discussion.posts)]
      [:h5.text-center statement-count]]
     [:div.mx-4
      [:small.text-primary (labels :discussion.members)]
      [:h5.text-center user-count]]]))

(defn graph-button
  "Rounded square button to navigate to the graph view"
  [share-hash]
  [:button.btn.btn-sm.btn-outline-primary.shadow-sm.mx-auto.rounded-1.h-100
   {:on-click #(rf/dispatch
                 [:navigation/navigate :routes/graph-view
                  {:share-hash share-hash}])}
   [:img
    {:src (img-path :icon-graph) :alt (labels :graph.button/text)
     :title (labels :graph.button/text)
     :height "30px"}]
   [:div (labels :graph.button/text)]])

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

(defn summary-button
  "Button to navigate to the summary view."
  [share-hash]
  (let [groups @(rf/subscribe [:user/groups])
        beta-user? (some shared-conf/beta-tester-groups groups)]
    [:button.btn.btn-sm.btn-outline-primary.shadow-sm.mx-auto.rounded-1.h-100
     (if beta-user?
       {:href (reitfe/href :routes.schnaq/summary {:share-hash share-hash})}
       {:on-click #(rf/dispatch [:modal {:show? true
                                         :child [beta-only-modal]}])})
     [:i {:style {:font-size "30px"}
          :class (str "m-auto fas fa-lg " (fa :text-width))}]
     [:p.m-0 (labels :summary.link.button/text)]]))

(defn navbar-statements []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.d-flex.flex-row.schnaq-navbar-space.mb-4
     [:div.d-flex.align-items-center.schnaq-navbar.px-4.ml-auto.mr-4
      [:div.mx-2
       [admin/share-link]]
      [admin/txt-export share-hash title]
      (when edit-hash
        [admin/admin-center share-hash edit-hash])
      [um/user-handling-menu "btn-outline-primary"]]
     [:div.d-flex.flex-row
      [:div.mx-2 [summary-button edit-hash]]
      [:div.mx-2 [graph-button edit-hash]]]]))