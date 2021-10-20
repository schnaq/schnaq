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
        [:div.d-md-none {:class title-class} (toolbelt/truncate-to-n-chars title 25)]]]])))

(defn- schnaq-logo []
  [:<>
   [:img.schnaq-brand-logo.align-middle.mr-2.d-md-none.d-none.d-xxl-block
    {:src (img-path :logo-white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]
   [:img.schnaq-brand-logo.align-middle.mr-2.d-xxl-none
    {:src (img-path :schnaqqifant/white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]])

(defn- navbar-title [additional-content]
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.mr-2.bg-white
   [:a.schnaq-logo-container.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
    [schnaq-logo]]
   [:div.mx-4
    [clickable-title]]
   additional-content])

(defn- navbar-qanda-title []
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.mr-2
   [:a.p-3.d-flex.h-100 {:href (reitfe/href :routes.schnaqs/personal)}
    [schnaq-logo]]
   [:div.mx-1.mx-md-5.px-md-5.pt-2.flex-column
    [clickable-title "text-white" "text-white"]]])

(defn additional-label-counter [label count]
  [:div.mx-4.ml-auto.d-none.d-xxl-block
   [:small.text-primary label]
   [:h5.text-center count]])

(defn- navbar-item [label item]
  [:div.d-flex.align-items-center [:small.d-lg-none (labels label)] item])

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
    [navbar-item
     :discussion.navbar/views
     [tooltip/text
      (labels :discussion.navbar/views)
      [:div.dropdown
       [navbar-components/separated-button
        [:div.dropdown-toggle
         [:img.header-standalone-icon
          {:src (img-path :icon-views-dark) :alt "graph icon"}]]
        {:id dropdown-id :data-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [:div.dropdown-menu.dropdown-menu-right {:aria-labelledby dropdown-id}
         [standard-view-button]
         [graph-button]
         [summary-button]
         [qanda-view-button]]]]]]))

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

(defn- progress-bar-hide-lg []
  [:div.d-lg-none.d-xl-block
   [schnaq-progress-bar]])

(defn navbar-settings []
  (let [{:discussion/keys [share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [navbar-item :graph.settings/title [graph-settings/open-settings]]
      (when edit-hash
        [navbar-item :schnaq.admin/tooltip [admin/admin-center]]))))

(defn- navbar-download []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [navbar-item :graph.download/as-png [admin/graph-download-as-png (gstring/format "#%s" graph-settings/graph-id)]]
      [navbar-item :schnaq.export/as-text [admin/txt-export share-hash title]])))

(defn- statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:div.pl-2
     [navbar-components/separated-button
      [:<>
       [motion/pulse-once [icon :comment/alt]
        [:schnaq.qa.new-question/pulse?]
        [:schnaq.qa.new-question/pulse false]
        (:secondary colors)]
       " "
       number-of-questions]
      {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])}]]))

(rf/reg-event-db
  :schnaq.qa.new-question/pulse
  (fn [db [_ pulse]]
    (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
  :schnaq.qa.new-question/pulse?
  (fn [db _]
    (get-in db [:schnaq :qa :new-question :pulse] false)))

(defn- language-toggle []
  [navbar-item :nav.buttons/language-toggle
   [:div.dropdown.ml-3
    [navbar-components/language-toggle-with-tooltip false {:class "text-dark btn-lg"}]]])

(defn share-modal []
  (if @(rf/subscribe [:schnaq.mode/qanda?])
    [navbar-item :sharing/tooltip [share-modal/share-qanda-button]]
    [navbar-item :sharing/tooltip [share-modal/share-discussion-button]]))

(defn- title-and-infos []
  (let [discussion @(rf/subscribe [:schnaq/selected])
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [navbar-title
     [:<>
      [additional-label-counter (labels :discussion.navbar/posts) statement-count]
      [additional-label-counter (labels :discussion.navbar/members) user-count]]]))

(defn- user-button []
  [:div.d-flex.align-items-center
   [um/user-handling-menu "btn-link text-dark"]])

(defn- collapsable-nav-bar [brand-content background-class & nav-key-items]
  (let [collapse-content-id "navbarSupportedContent"]
    [:nav.navbar.navbar-expand-lg.navbar-light.schnaq-navbar-dynamic-padding
     [:navbar-brand.p-0 {:href "#"} brand-content]
     [:button.navbar-toggler.ml-2
      {:type "button" :data-toggle "collapse"
       :data-target (str "#" collapse-content-id)
       :aria-controls collapse-content-id :aria-expanded "false" :aria-label "Toggle navigation"}
      [:span.navbar-toggler-icon]]
     [:div.collapse.navbar-collapse {:id collapse-content-id}
      [:ul.navbar-nav.rounded-1.ml-auto.d-flex.align-items-center.px-2
       {:class background-class}
       [:li.nav-item.ml-auto.d-lg-none [user-button]]
       (for [[nav-item key] nav-key-items]
         [:li.nav-item.ml-auto.m-lg-0.pl-2 {:key (str "nav-item-" key)} nav-item])
       [:li.nav-item.d-none.d-lg-block [user-button]]]]]))

(defn header []
  [collapsable-nav-bar
   [title-and-infos]
   "navbar-bg-white-sm-transparent"
   [[progress-bar-hide-lg] "Progress-Bar"]
   [[share-modal] "Share-Modal"]
   [[navbar-download] "Navbar-Download"]
   [[navbar-settings] "Navbar-Settings"]
   [[language-toggle] "Language-Toggle"]
   [[dropdown-views] "Dropdown-Views"]])

(defn header-for-qanda-view []
  [collapsable-nav-bar
   [navbar-qanda-title]
   "bg-transparent"
   [[language-toggle] "Language-Toggle"]
   [[statement-counter] "Statement-Counter"]])

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
