(ns schnaq.interface.views.navbar.collapse-content
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.navbar :as navbar]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.discussion.share :refer [share-schnaq-button]]
            [schnaq.interface.views.navbar.elements :as nav-elements :refer [language-dropdown]]
            [schnaq.interface.views.navbar.user-management :as um]))

(defn- list-element-href-button-builder
  "Build buttons as list element hyperlinks."
  [label icon route share-hash]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        is-active? (= current-route route)
        href (navigation/href route {:share-hash share-hash})
        classes (if is-active?
                  {:href href :class "text-primary"}
                  {:href href})]
    [:a.li.list-group-item.list-group-item-action classes
     [:img.navbar-view-toggle.me-3
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
  [props content]
  [:button.list-group-item.list-group-item-action
   props content])

(defn- settings-li-button
  "Either display schnaq settings or graph settings button."
  []
  (let [{:discussion/keys [share-hash]} @(rf/subscribe [:schnaq/selected])
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [li-button {:on-click #(nav-elements/show-notification)} (labels :graph.settings/title)]
      (when @(rf/subscribe [:user/moderator?])
        [:a.button.list-group-item.list-group-item-action
         {:href (navigation/href :routes.schnaq/moderation-center {:share-hash share-hash})}
         (labels :schnaq.admin/tooltip)]))))

(defn- views
  "Display all views as a list-group"
  []
  [:<>
   [:div.fw-bold.mt-3 (labels :discussion.navbar/views)]
   [:ul.list-group.list-group-flush
    [standard-view-li]
    [graph-li]
    [summary-li]
    [qanda-view-li]]])

(defn- settings
  "Display all settings as a list group"
  []
  [:<>
   [:div.fw-bold.mt-3 (labels :discussion.navbar/settings)]
   [:ul.list-group.list-group-flush
    [share-schnaq-button (fn [props] [li-button props (labels :sharing/tooltip)])]
    [:li.list-group-item.dropdown [language-dropdown true {:class "p-0 text-dark"}]]
    [settings-li-button]]])

(defn- external-content [collapse-content-id content]
  [:div.collapse.navbar-collapse.bg-white.p-2.m-1.rounded-2.d-lg-none
   {:id collapse-content-id}
   content])

(defn navbar-external-content
  "External content to display in a collapsible section of a navigation bar."
  [collapse-content-id]
  [external-content collapse-content-id
   [:<>
    [nav-elements/user-button true]
    [views]
    [settings]]])

(defn- li-link-button
  [label href]
  [:a.list-group-item.list-group-item-action
   {:href href}
   (labels label)])

(defn collapsed-navbar
  "Collapsible content for schnaq overview. Used in the collapsed navbar."
  [collapse-content-id]
  [external-content collapse-content-id
   [:<>
    [um/register-or-user-button true]
    [:ul.list-group.list-group-flush
     [li-link-button :router/pricing "https://schnaq.com/pricing"]
     [li-link-button :router/privacy "https://schnaq.com/en/privacy"]
     [li-link-button :nav/blog "https://schnaq.com/blog/"]
     [:li.list-group-item.dropdown [navbar/language-dropdown true {:class "p-0 text-dark"}]]]]])
