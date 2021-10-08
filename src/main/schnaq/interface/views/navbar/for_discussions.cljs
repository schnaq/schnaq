(ns schnaq.interface.views.navbar.for-discussions
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.utils.time :as time]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.graph.settings :as graph-settings]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn clickable-title []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])]
    [:<>
     [:small.text-primary (labels :discussion.navbar/title)]
     [:div.clickable-no-hover
      [:a.link-unstyled
       {:href (reitfe/href :routes.schnaq/start {:share-hash share-hash})}
       [:h1.h5 (toolbelt/truncate-to-n-chars title 30)]]]]))

(defn navbar []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))
        current-route @(rf/subscribe [:navigation/current-route-name])
        qanda? (= current-route :routes.schnaq/qanda)]
    [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4
     ;; schnaq logo
     [:a.schnaq-logo-container.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
      [:img.d-inline-block.align-middle.mr-2
       {:src (img-path :logo-white) :alt "schnaq logo"
        :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]
     [:div.mx-4
      [clickable-title]]
     (when-not qanda?
       [:<>
        [:div.mx-4.ml-auto.d-none.d-md-block
         [:small.text-primary (labels :discussion.navbar/posts)]
         [:h5.text-center statement-count]]
        [:div.mx-4.d-none.d-md-block
         [:small.text-primary (labels :discussion.navbar/members)]
         [:h5.text-center user-count]]])]))


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

(defn- graph-button
  "Rounded square button to navigate to the graph view"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :graph.button/text :icon-graph-dark
     (reitfe/href :routes/graph-view {:share-hash share-hash})]))

(defn- summary-button
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

(defn- dropdown-views []
  (let [dropdown-id "schnaq-views-dropdown"]
    [:div.dropdown
     [:button.btn.btn-white.discussion-navbar-button
      {:id dropdown-id :type "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      [:img.header-standalone-icon
       {:src (img-path :icon-views-dark) :alt "graph icon"}]
      [:p.small.m-0.text-nowrap.dropdown-toggle (labels :discussion.navbar/views)]]
     [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
      [standard-view-button]
      [graph-button]
      [summary-button]
      [qanda-view-button]]]))

;; -----------------------------------------------------------------------------

(>defn- schnaq-progress-information
  "Take the time the schnaq was created and the end-time and returns the percentage for the progress bar as well
  as whole days left."
  [created-at end-time]
  [inst? inst? :ret (s/tuple float? int?)]
  (let [distance (- end-time created-at)
        elapsed-ms (min distance (- (.now js/Date) created-at))
        elapsed-percent (* 100 (/ elapsed-ms distance))
        days-left (jsw/number-trunc (/ (max 0 (- distance elapsed-ms)) 86400000))]
    [elapsed-percent days-left]))

(defn- schnaq-progress-bar
  "A progress bar indicating how far along a schnaq is."
  []
  (let [{:discussion/keys [end-time created-at]} @(rf/subscribe [:schnaq/selected])
        [current-bar days-left] (schnaq-progress-information created-at end-time)
        progress-text (cond
                        (nil? end-time) (labels :discussion.progress/unlimited)
                        (< end-time (.now js/Date)) (labels :discussion.progress/end)
                        (inst? end-time) (gstring/format (labels :discussion.progress/days-left) days-left))
        [first-word & rest] (str/split progress-text #" ")]
    [tooltip/text
     (if end-time
       (gstring/format (labels :discussion.progress/ends) (time/formatted-with-timezone end-time))
       (labels :discussion.progress/ends-not))
     [:section
      [:p.small.m-0 [:span.font-color-primary first-word " "] (str/join " " rest)]
      [:div.progress.progress-schnaq.mr-3
       [:div.progress-bar.progress-bar-schnaq
        (cond->
          {:role "progressbar" :aria-valuenow (str current-bar) :aria-valuemin "0" :aria-valuemax "100"
           :style {:width (str current-bar "%")}}
          (nil? end-time) (assoc :class "progress-bar-striped"))]]]]))

(defn navbar-settings []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [:<>
       [graph-settings/open-settings]
       [admin/graph-download-as-png (gstring/format "#%s" graph-settings/graph-id)]]
      [:<>
       [admin/txt-export share-hash title]
       (when edit-hash
         [admin/admin-center])])))

(defn navbar-statements []
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        qanda? (= current-route :routes.schnaq/qanda)]
    [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-xl-auto
     (when-not qanda?
       [:div.d-flex.align-items-center.schnaq-navbar.px-4.mb-4.mb-md-0
        [schnaq-progress-bar]
        [admin/share-link]
        [navbar-settings]
        [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn-lg"}]])
     [:div.d-flex.align-items-center
      [:div.mr-2.mx-md-2 [dropdown-views]]
      [:div.d-flex.align-items-center.schnaq-navbar
       [um/user-handling-menu "btn-link"]]]]))

(defn header
  "Overview header for a discussion."
  []
  [:div.d-flex.flex-row.flex-wrap.p-md-3
   [navbar]
   [navbar-statements]])

(defn embeddable-header []
  ;; The view breaks earlier, because the breakpoints heed the screen size, not the div size
  (let [{:discussion/keys [title share-hash] :as discussion} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:div.d-flex.flex-row.flex-wrap.p-md-3
     [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4.mr-3
      ;; schnaq logo
      [:div.mx-4.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/posts)]
       [:h5.text-center statement-count]]
      [:div.mx-4.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/members)]
       [:h5.text-center user-count]]
      [:a.schnaq-logo-container.d-flex.h-100.text-decoration-none
       {:href (reitfe/href :routes.schnaq/start)}
       [:small.text-white "powered by"]
       [:img.d-inline-block.align-middle.mr-2
        {:src (img-path :logo-white) :alt "schnaq logo"
         :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]]
     [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-xl-auto
      [:div.d-flex.align-items-center.schnaq-navbar.px-4
       [schnaq-progress-bar]
       [admin/txt-export share-hash title]
       (when edit-hash
         [admin/admin-center])
       [navbar-components/language-toggle-with-tooltip false {:class "btn-lg"}]]
      [:div.d-flex.align-items-center.mt-4.mt-md-0
       [:div.mx-2.embedded-nav-button [graph-button]]
       [:div.mr-2.embedded-nav-button [summary-button]]]]]))
