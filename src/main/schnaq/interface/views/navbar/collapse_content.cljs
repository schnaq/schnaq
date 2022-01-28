(ns schnaq.interface.views.navbar.collapse-content
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.discussion.share :as share-modal]
            [schnaq.interface.views.graph.settings :as graph-settings]
            [schnaq.interface.views.navbar.elements :as nav-elements]
            [schnaq.interface.views.navbar.user-management :as um]))

(defn- list-element-href-button-builder
  "Build buttons as list element hyperlinks."
  [label icon route share-hash]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        is-active? (= current-route route)
        href (reitfe/href route {:share-hash share-hash})
        classes (if is-active?
                  {:href href :class "text-primary"}
                  {:href href})]
    [:a.li.list-group-item.list-group-item-action classes
     [:img.navbar-view-toggle.mr-3
      {:src (img-path icon)
       :alt "graph icon"}]
     (labels label)]))

(defn- graph-li
  "List button to navigate to the graph view"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [list-element-href-button-builder
     :graph.button/text :icon-graph-dark
     :routes/graph-view share-hash]))

(defn- summary-li
  "List button to navigate to the summary view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [list-element-href-button-builder
     :summary.link.button/text :icon-summary-dark
     :routes.schnaq/dashboard share-hash]))

(defn- standard-view-li
  "List button to navigate to the standard overview."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [list-element-href-button-builder
     :discussion.button/text :icon-cards-dark
     :routes.schnaq/start share-hash]))

(defn- qanda-view-li
  "List button to navigate to the Q&A view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [list-element-href-button-builder
     :qanda.button/text :icon-qanda-dark
     :routes.schnaq/qanda share-hash]))

(defn- li-button
  "List element standard button."
  [content on-click-fn]
  [:button.list-group-item.list-group-item-action
   {:on-click on-click-fn}
   content])

(defn- settings-li-button
  "Either display schnaq settings or graph settings button."
  []
  (let [{:discussion/keys [share-hash]} @(rf/subscribe [:schnaq/selected])
        edit-hash @(rf/subscribe [:schnaq.current/admin-access])
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [li-button (labels :graph.settings/title) (fn [_] (graph-settings/show-notification))]
      (when edit-hash
        [:a.button.list-group-item.list-group-item-action
         {:href (reitfe/href :routes.schnaq/admin-center
                             {:share-hash share-hash :edit-hash edit-hash})}
         (labels :schnaq.admin/tooltip)]))))

(defn- user-bar
  "Display the user avatar."
  []
  [:div.d-flex.align-items-center.ml-auto
   [nav-elements/user-button]])

(defn- views
  "Display all views as a list-group"
  []
  [:<>
   [:div.font-weight-bold.mt-3 (labels :discussion.navbar/views)]
   [:ul.list-group.list-group-flush
    [standard-view-li]
    [graph-li]
    [summary-li]
    [qanda-view-li]]])

(defn- settings
  "Display all settings as a list group"
  []
  [:<>
   [:div.font-weight-bold.mt-3 (labels :discussion.navbar/settings)]
   [:ul.list-group.list-group-flush
    [li-button (labels :sharing/tooltip) (fn [_] (share-modal/open-share-discussion))]
    [:li.list-group-item.dropdown [nav-elements/language-with-label-dropdown]]
    [settings-li-button]]])

(defn- external-content [collapse-content-id content]
  [:div.collapse.bg-white.p-2.m-1.rounded-2.d-lg-none
   {:id collapse-content-id}
   content])

(defn navbar-external-content
  "External content to display in a collapsible section of a navigation bar."
  [collapse-content-id]
  [external-content collapse-content-id
   [:<>
    [user-bar]
    [views]
    [settings]]])

(defn- li-link-button
  [label href]
  [:a.list-group-item.list-group-item-action
   {:href href}
   (labels label)])

(defn navbar-external-overview-content
  "Collapsible content for schnaq overview."
  [collapse-content-id]
  [external-content collapse-content-id
   [:<>
    [:div.d-flex.align-items-center
     [:div.ml-auto
      [um/register-handling-menu "btn-link"]]]
    [:ul.list-group.list-group-flush
     [li-link-button :router/startpage (reitfe/href :routes/startpage)]
     [li-link-button :router/pricing (reitfe/href :routes/pricing)]
     [li-link-button :router/privacy (reitfe/href :routes/privacy)]
     [li-link-button :nav/blog "https://schnaq.com/blog/"]
     [:li.list-group-item.dropdown [nav-elements/language-with-label-dropdown]]]]])


