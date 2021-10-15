(ns schnaq.interface.views.navbar.for-discussions
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.utils.time :as util-time]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.share :as share-modal]
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

(defn- navbar-title [additional-content]
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4
   ;; schnaq logo
   [:a.schnaq-logo-container.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
    [:img.d-inline-block.align-middle.mr-2
     {:src (img-path :logo-white) :alt "schnaq logo"
      :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]
   [:div.mx-4
    [clickable-title]]
   additional-content])

(defn- navbar-title-with-meta-infos []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [navbar-title
     [:<>
      [:div.mx-4.ml-auto.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/posts)]
       [:h5.text-center statement-count]]
      [:div.mx-4.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/members)]
       [:h5.text-center user-count]]]]))


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
    [:div.dropdown.pl-2
     [navbar-components/separated-button
      [:<>
       [:img.header-standalone-icon
        {:src (img-path :icon-views-dark) :alt "graph icon"}]
       [:p.small.m-0.text-nowrap.dropdown-toggle (labels :discussion.navbar/views)]]
      {:id dropdown-id :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
       [standard-view-button]
       [graph-button]
       [summary-button]
       [qanda-view-button]]]]))

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
       (gstring/format (labels :discussion.progress/ends) (util-time/formatted-with-timezone end-time))
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

(defn- statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])
        qa? @(rf/subscribe [:schnaq.mode/qanda?])
        share-hash @(rf/subscribe [:schnaq/share-hash])
        current-route @(rf/subscribe [:navigation/current-route-name])]
    (when (and qa? (= :routes.schnaq/qanda current-route))
      [:div.pl-2
       [navbar-components/separated-button
        [:<>
         [motion/pulse-once [icon :comment/alt]
          [:schnaq.qa.new-question/pulse?]
          [:schnaq.qa.new-question/pulse false]
          (:secondary colors)]
         " "
         number-of-questions]
        {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])}]])))

(rf/reg-event-db
  :schnaq.qa.new-question/pulse
  (fn [db [_ pulse]]
    (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
  :schnaq.qa.new-question/pulse?
  (fn [db _]
    (get-in db [:schnaq :qa :new-question :pulse] false)))

(defn navbar-tools
  "Showing utilities in the navbar. E.g. Dropdown of views and user-menu."
  [content]
  [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-xl-auto
   (when content
     [:div.d-flex.align-items-center.schnaq-navbar.px-4.mb-4.mb-md-0
      content])
   [:div.d-flex.align-items-center
    [statement-counter]
    [dropdown-views]
    [:div.d-flex.align-items-center.schnaq-navbar.ml-2
     [um/user-handling-menu "btn-link"]]]])

(defn- header-discussion
  "Overview header for a discussion."
  []
  [:div.d-flex.flex-row.flex-wrap.p-md-3
   [navbar-title-with-meta-infos]
   [navbar-tools
    [:<>
     [schnaq-progress-bar]
     [share-modal/share-discussion-button]
     [navbar-settings]
     [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn-lg"}]]]])

(defn- header-qanda
  "Overview header for a discussion."
  []
  [:div.d-flex.flex-row.flex-wrap.p-md-3
   [navbar-title-with-meta-infos]
   [navbar-tools
    [:<>
     [schnaq-progress-bar]
     [share-modal/share-qanda-button]
     [navbar-settings]
     [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn-lg"}]]]])

(defn header
  "Header to dispatch between Q&A and discussion"
  []
  (if @(rf/subscribe [:schnaq.mode/qanda?])
    [header-qanda]
    [header-discussion]))

(defn- navbar-qanda-title []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])]
    [:<>
     [:div.schnaq-navbar.mb-4.bg-primary
      [:a.p-3.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
       [:img.d-inline-block.align-middle
        {:src (img-path :logo-white) :alt "schnaq logo"
         :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]]
     [:div.mx-1.mx-md-5.px-md-5.pt-2.d-flex.flex-grow-1.flex-column
      [:small.text-white (labels :discussion.navbar/title)]
      [:div.clickable-no-hover
       [:a.link-unstyled
        {:href (reitfe/href :routes.schnaq/start {:share-hash share-hash})}
        [:h1.h5.text-white (toolbelt/truncate-to-n-chars title 40)]]]]]))

(defn header-for-qanda-view
  "Header displaying only title, views and user"
  []
  [:div.d-flex.flex-row.flex-wrap.p-md-3
   [navbar-qanda-title]
   [navbar-tools]])

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
