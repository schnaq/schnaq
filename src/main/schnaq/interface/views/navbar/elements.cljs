(ns schnaq.interface.views.navbar.elements
  (:require [com.fulcrologic.guardrails.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.graph.settings :as graph-settings]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn clickable-title
  ([]
   [clickable-title "text-primary" "text-dark"])
  ([label-class title-class]
   (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])]
     [:<>
      [:small {:class label-class} (labels :discussion.navbar/title)]
      [:div.clickable-no-hover
       [:a.link-unstyled
        {:href (reitfe/href :routes.schnaq/start {:share-hash share-hash})}
        [:h1.h5.d-none.d-md-block {:class title-class} (toolbelt/truncate-to-n-chars title 25)]
        [:div.d-md-none {:class title-class} (toolbelt/truncate-to-n-chars title 22)]]]])))

(defn- schnaq-logo []
  [:<>
   [:img.schnaq-brand-logo.align-middle.mr-2.d-md-none.d-none.d-xxl-block
    {:src (img-path :logo-white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]
   [:img.schnaq-brand-logo.align-middle.mr-2.d-xxl-none
    {:src (img-path :schnaqqifant/white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]])

(defn navbar-title
  "Brand logo and title with dynamic resizing."
  [title additional-content]
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.mr-2.bg-white
   [:a.schnaq-logo-container.d-flex.h-100 {:href (toolbelt/current-overview-link)}
    [schnaq-logo]]
   [:div.mx-0.mx-md-4
    title]
   additional-content])

(defn navbar-qanda-title []
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.mr-2
   [:a.p-3.d-flex.h-100 {:href (toolbelt/current-overview-link)}
    [schnaq-logo]]
   [:div.mx-1.mx-md-5.px-md-5.pt-2.flex-column
    [clickable-title "text-white" "text-white"]]])

(defn additional-label-counter [label count]
  [:div.mx-4.ml-auto.d-none.d-xxl-block
   [:small.text-primary label]
   [:h5.text-center count]])

;; -----------------------------------------------------------------------------

(>defn- discussion-button-builder
  "Build buttons in the discussion navigation."
  [label icon href]
  [keyword? keyword? fn? :ret vector?]
  [:a.dropdown-item {:href href}
   [:div.text-center
    [:img.header-standalone-icon
     {:src (img-path icon)
      :alt "graph icon"}]
    [:p.small.m-0.text-nowrap (labels label)]]])

(defn graph-button
  "Rounded square button to navigate to the graph view"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :graph.button/text :icon-graph-dark
     (reitfe/href :routes/graph-view {:share-hash share-hash})]))

(defn summary-button
  "Button to navigate to the summary view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :summary.link.button/text :icon-summary-dark
     (reitfe/href :routes.schnaq/dashboard {:share-hash share-hash})]))

(defn- standard-view-button
  "Button to navigate to the standard overview."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :discussion.button/text :icon-cards-dark
     (reitfe/href :routes.schnaq/start {:share-hash share-hash})]))

(defn- qanda-view-button
  "Button to navigate to the Q&A view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :qanda.button/text :icon-qanda-dark
     (reitfe/href :routes.schnaq/qanda {:share-hash share-hash})]))

(defn dropdown-views
  "Displays a Dropdown menu button for the available views"
  ([]
   [dropdown-views :icon-views-dark ""])
  ([icon-id toggle-class]
   (let [dropdown-id "schnaq-views-dropdown"]
     [tooltip/text
      (labels :discussion.navbar/views)
      [:div.dropdown
       [navbar-components/separated-button
        [:div.dropdown-toggle
         {:class toggle-class}
         [:img.header-standalone-icon
          {:src (img-path icon-id) :alt "graph icon"}]]
        {:id dropdown-id :data-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
         [standard-view-button]
         [graph-button]
         [summary-button]
         [qanda-view-button]]]]])))

;; -----------------------------------------------------------------------------

(defn navbar-settings
  "Either display schnaq or graph settings button"
  []
  (let [{:discussion/keys [share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [graph-settings/open-settings]
      (when edit-hash
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
    [:div
     [navbar-components/separated-button
      [:div.d-flex.text-white
       [motion/pulse-once [icon :comment/alt]
        [:schnaq.qa.new-question/pulse?]
        [:schnaq.qa.new-question/pulse false]
        (:white colors)
        (:secondary colors)]

       [:div.ml-2 number-of-questions]]
      {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])}]]))

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
  [:div.dropdown.ml-3
   [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn-lg"}]])

(defn title-and-infos
  "Display the schnaq title and info"
  []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [navbar-title
     [clickable-title]
     [:<>
      [additional-label-counter (labels :discussion.navbar/posts) statement-count]
      [additional-label-counter (labels :discussion.navbar/members) user-count]]]))

(defn user-button
  "Display the user settings button"
  ([]
   [user-button "btn-link text-dark"])
  ([classes]
   [:div.d-flex.align-items-center
    [um/user-handling-menu classes]]))

(defn language-with-label-dropdown
  "Display a language dropdown menu with a description label"
  []
  (let [dropdown-id "schnaq-collapsed-language-dropdown"]
    [:<>
     [:nav-link.dropdown-toggle
      {:id dropdown-id
       :href "#" :role "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      (labels :nav.buttons/language-toggle) " "
      [icon :language]]
     [:div.dropdown-menu {:aria-labelledby dropdown-id}
      [:button.dropdown-item
       {:on-click #(language/set-language :de)} "Deutsch"]
      [:button.dropdown-item
       {:on-click #(language/set-language :en)} "English"]
      [:button.dropdown-item
       {:on-click #(language/set-language :pl)} "Polski"]]]))
