(ns schnaq.interface.views.navbar.for-discussions
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.utils.toolbelt :as toolbelt]
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
        user-count (count (:authors meta-info))]
    [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4
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
  [:a {:href (reitfe/href :routes/graph-view {:share-hash share-hash})}
   [:button.btn.btn-sm.btn-dark-highlight.shadow-sm.rounded-1.h-100
    [:img.header-standalone-icon
     {:src (img-path :icon-graph)
      :alt "graph icon"}]
    [:p.small.m-0 (labels :graph.button/text)]]])

(defn summary-button
  "Button to navigate to the summary view."
  [share-hash]
  [:a {:href (reitfe/href :routes.schnaq/dashboard {:share-hash share-hash})}
   [:button.btn.btn-sm.btn-white.mx-auto.rounded-1.h-100
    [:img.img-fluid
     {:src (img-path :icon-summary-dark)
      :width "25"
      :alt "summary icon"}]
    [:p.small.m-0 (labels :summary.link.button/text)]]])


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
    [:section
     [:p.small.m-0 [:span.font-color-primary first-word " "] (str/join " " rest)]
     [:div.progress.progress-schnaq.mr-3
      [:div.progress-bar.progress-bar-schnaq
       (cond->
         {:role "progressbar" :aria-valuenow (str current-bar) :aria-valuemin "0" :aria-valuemax "100"
          :style {:width (str current-bar "%")}}
         (nil? end-time) (assoc :class "progress-bar-striped"))]]]))

(defn navbar-statements []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-xxl-auto
     [:div.d-flex.align-items-center.schnaq-navbar.px-md-4.mb-4.mb-lg-0
      [schnaq-progress-bar]
      [admin/share-link]
      [admin/txt-export share-hash title]
      (when edit-hash
        [admin/admin-center share-hash edit-hash])]
     [:div.d-flex.align-items-center
      [:div.h-100.mr-2.mx-lg-2 [graph-button share-hash]]
      [:div.h-100.mr-2 [summary-button share-hash]]
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
     [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-hd-auto
      [:div.d-flex.align-items-center.schnaq-navbar.px-4.mb-4.mb-md-0
       [schnaq-progress-bar]
       [admin/txt-export share-hash title]
       (when edit-hash
         [admin/admin-center share-hash edit-hash])]
      [:div.d-flex.align-items-center.mt-4.mt-md-0
       [:div.h-100.mx-2 [graph-button share-hash]]
       [:div.h-100.mr-2 [summary-button share-hash]]]]]))
