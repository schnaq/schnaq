(ns schnaq.interface.views.navbar.elements
  (:require [com.fulcrologic.guardrails.core :refer [>defn- ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.graph.settings :as graph-settings]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn- clickable-title
  ([]
   [clickable-title "text-dark"])
  ([title-class]
   (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])]
     [:div.clickable-no-hover
      [:a.link-unstyled
       {:href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
       [:h1.h6.d-none.d-md-block.text-wrap {:class title-class} (toolbelt/truncate-to-n-chars title 64)]
       [:div.d-md-none {:class title-class} (toolbelt/truncate-to-n-chars title 32)]]])))

(defn- schnaq-logo []
  [:<>
   [:img.schnaq-brand-logo.align-middle.me-2.d-md-none.d-none.d-xxl-block
    {:src (img-path :logo-white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]
   [:img.schnaq-brand-logo.align-middle.me-2.d-xxl-none
    {:src (img-path :schnaqqifant/white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]])

(defn navbar-title
  "Brand logo and title with dynamic resizing."
  ([title]
   [navbar-title title true])
  ([title clickable-title?]
   [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.me-2.bg-white
    [:a.schnaq-logo-container.d-flex.h-100 (when clickable-title?
                                             {:href (navigation/href :routes/startpage)})
     [schnaq-logo]]
    [:div.mx-0.mx-md-4.text-wrap title]
    [:div.h-100.d-none.d-md-block.p-2
     [common-components/theme-logo {:style {:max-height "100%"}}]]]))

(defn navbar-qanda-title []
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.me-2
   [:a.p-3.d-flex.h-100 {:href (toolbelt/current-overview-link)}
    [schnaq-logo]]
   [:div.mx-1.mx-md-5.px-md-5.pt-2
    [clickable-title "text-white"]]
   [:div.d-none.d-md-inline
    [common-components/theme-logo {:style {:max-width "150px"}}]]])

;; -----------------------------------------------------------------------------

(>defn- discussion-button-builder
  "Build buttons in the discussion navigation."
  [label icon href]
  [keyword? keyword? (? string?) :ret vector?]
  [:a.dropdown-item {:href href}
   [:div.text-center
    [:img.navbar-view-toggle
     {:src (img-path icon)
      :alt "graph icon"}]
    [:p.small.m-0.text-nowrap (labels label)]]])

(defn graph-button
  "Rounded square button to navigate to the graph view"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :graph.button/text :icon-graph-dark
     (navigation/href :routes/graph-view {:share-hash share-hash})]))

(defn summary-button
  "Button to navigate to the summary view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :summary.link.button/text :icon-summary-dark
     (navigation/href :routes.schnaq/dashboard {:share-hash share-hash})]))

(defn- standard-view-button
  "Button to navigate to the standard overview."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :discussion.button/text :icon-cards-dark
     (navigation/href :routes.schnaq/start {:share-hash share-hash})]))

(defn- qanda-view-button
  "Button to navigate to the Q&A view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :qanda.button/text :icon-qanda-dark
     (navigation/href :routes.schnaq/qanda {:share-hash share-hash})]))

(defn dropdown-views
  "Displays a Dropdown menu button for the available views"
  ([]
   [dropdown-views :icon-views-dark ""])
  ([icon-id toggle-class]
   (let [dropdown-id "schnaq-views-dropdown"
         current-route @(rf/subscribe [:navigation/current-route-name])]
     [tooltip/text
      (labels :discussion.navbar/views)
      [:div.dropdown
       [navbar-components/separated-button
        [:div.dropdown-toggle
         {:class toggle-class}
         [:img.navbar-view-toggle.d-block
          {:src (img-path icon-id) :alt (labels :navbar.icon.views/alt-text)}]
         [:span.small
          (case current-route
            :routes.schnaq/start (labels :discussion.button/text)
            :routes.schnaq.select/statement (labels :discussion.button/text)
            :routes/graph-view (labels :graph.button/text)
            :routes.schnaq/dashboard (labels :summary.link.button/text)
            :routes.schnaq/qanda (labels :qanda.button/text)
            (labels :discussion.navbar/views))]]
        {:id dropdown-id :data-bs-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby dropdown-id}
         [standard-view-button]
         [graph-button]
         [summary-button]
         [qanda-view-button]]]]])))

;; -----------------------------------------------------------------------------

(defn navbar-settings
  "Either display schnaq or graph settings button"
  []
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)
        admin? @(rf/subscribe [:schnaq.current/admin-access])]
    (if graph?
      [graph-settings/open-settings]
      (when admin?
        [admin/admin-center]))))

(defn navbar-download
  "Download button for either text or graph"
  []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [admin/graph-download-as-png (gstring/format "#%s" graph-settings/graph-id)]
      [admin/txt-export share-hash title])))

(defn statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:a
     {:href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
     [navbar-components/separated-button
      [:div.d-flex.text-white
       [motion/pulse-once [icon :comment/alt]
        [:schnaq.qa.new-question/pulse?]
        [:schnaq.qa.new-question/pulse false]
        (:white colors)
        (:secondary colors)]
       [:div.ms-2 number-of-questions]]]]))

(rf/reg-event-db
 :schnaq.qa.new-question/pulse
 (fn [db [_ pulse]]
   (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
 :schnaq.qa.new-question/pulse?
 (fn [db _]
   (get-in db [:schnaq :qa :new-question :pulse] false)))

(defn language-toggle
  "Language Toggle dropdown button"
  []
  [:div.dropdown
   [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn"}]])

(defn discussion-title
  "Display the schnaq title and info"
  []
  [navbar-title
   [clickable-title]])

(defn user-button
  "Display the user settings button"
  ([]
   [user-button false])
  ([on-white-background?]
   [:div.d-flex.align-items-center
    [um/user-dropdown-button on-white-background?]]))
