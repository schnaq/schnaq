(ns schnaq.interface.views.feed.overview
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.feed.filters :as filters]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user :as user]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [motion/delayed-fade-in
   [:div.alert.alert-light.text-light.row.blue-wave-background.p-md-5.text-center
    [:div.col-2.py-md-5.d-flex
     [:div.display-1.align-self-center "🙈"]]
    [:div.col-10.py-md-5
     [:h2 (labels :schnaqs.not-found/alert-lead)]
     [:p (labels :schnaqs.not-found/alert-body)]
     [:a.btn.btn-outline-light.mt-1
      {:href (reitfe/href :routes.schnaq/create)}
      (labels :nav.schnaqs/create-schnaq)]]]])

(defn sort-options
  "Displays the different sort options for feed elements."
  []
  (let [sort-method @(rf/subscribe [:feed/sort])]
    [:span.pl-2.text-right
     [:button.btn.btn-outline-primary.btn-sm.mx-1
      {:class (when (= sort-method :time) "active")
       :on-click #(rf/dispatch [:feed.sort/set :time])}
      (labels :badges.sort/newest)]
     [:button.btn.btn-outline-primary.btn-sm
      {:class (when (= sort-method :alphabetical) "active")
       :on-click #(rf/dispatch [:feed.sort/set :alphabetical])}
      (labels :badges.sort/alphabetical)]]))

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq delete-from-hub?]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:article.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div.d-flex.flex-row
      [:div.highlight-card
       [:img.schnaq-header-image {:src url}]]
      [:div.row.px-md-4.py-2.w-100
       [:div.col-4.col-md-6
        [:div.row.ml-1
         [user/user-info-only (:discussion/author schnaq) 42]
         [:div.mt-2 [badges/read-only-badge schnaq]]]
        [:div.mt-1 [badges/static-info-badges schnaq]]]
       [:div.col-8.col-md-6
        [:div.meeting-entry-title
         (toolbelt/truncate-to-n-chars title 40)]]]
      (when delete-from-hub?
        [:button.btn.btn-outline-dark.btn-small.my-auto.mr-3
         {:title (labels :hub.remove.schnaq/tooltip)
          :on-click (fn [e] (js-wrap/stop-propagation e)
                      (when (js/confirm (labels :hub.remove.schnaq/prompt))
                        (rf/dispatch [:hub.remove/schnaq share-hash])))}
         [:i {:class (str "m-auto fas " (fa :cross))}]])]]))

(defn schnaq-list-view
  "Shows a list of schnaqs."
  ([subscription-vector]
   [schnaq-list-view subscription-vector false])
  ([subscription-vector show-delete-from-hub-button?]
   (let [schnaqs @(rf/subscribe subscription-vector)
         sort-method @(rf/subscribe [:feed/sort])
         active-filters @(rf/subscribe [:filters.discussion/active])
         filtered-schnaqs (filters/filter-discussions schnaqs active-filters)
         sorted-schnaqs (if (= :alphabetical sort-method)
                          (sort-by :discussion/title filtered-schnaqs)
                          (sort-by :discussion/created-at > filtered-schnaqs))]
     (if (empty? schnaqs)
       [no-schnaqs-found]
       [:div.panel-white.rounded-1.px-md-5
        [:div.row.pl-5
         [:div.col-7 [:p.text-muted (labels :schnaqs/author)]]
         [:div.col-5 [:p.text-muted (labels :schnaqs/schnaq)]]]
        [:div.row.mb-3
         [:div.col
          [sort-options]
          [filters/filter-button]]]
        (for [schnaq sorted-schnaqs]
          [:div.pb-4 {:key (:db/id schnaq)}
           [schnaq-entry schnaq show-delete-from-hub-button?]])]))))

(defn- feed-button
  "Create a button for the feed list."
  ([label route]
   [feed-button label route nil])
  ([label route route-params]
   (let [current-route @(rf/subscribe [:navigation/current-route-name])
         button-class (if (= current-route route) "feed-button-focused" "feed-button")]
     [:a.btn.btn-link.text-left {:class button-class
                                 :role "button"
                                 :href (reitfe/href route route-params)}
      (labels label)])))

(defn- generic-feed-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn sidebar-info-links []
  [:section.panel-white.text-center.mt-5
   [:div.btn-group {:role "group"}
    [:div.btn-group-vertical
     [generic-feed-button :coc/heading (reitfe/href :routes/code-of-conduct)]
     [generic-feed-button :how-to/button (reitfe/href :routes/how-to)]]]])

(defn feed-navigation
  "Navigate between the feeds."
  []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])
        hubs @(rf/subscribe [:hubs/all])]
    [:section.px-md-3
     [:div.panel-white.m-0
      (when hubs
        [:<>
         [hub/list-hubs-with-heading]
         [:hr.d-none.d-md-block]])
      [feed-button :router/visited-schnaqs :routes.schnaqs/personal]
      (when-not (nil? edit-hash)
        [feed-button :nav.schnaqs/last-added
         :routes.schnaq/admin-center {:share-hash share-hash :edit-hash edit-hash}])
      [feed-button :nav.schnaqs/create-schnaq :routes.schnaq/create]]
     [:div.d-none.d-md-block
      [sidebar-info-links]]]))

(defn- personal-discussions-view
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  []
  [pages/three-column-layout
   {:page/heading (labels :schnaqs/header)
    :page/subheading (labels :schnaqs/subheader)}
   [feed-navigation]
   [schnaq-list-view [:schnaqs.visited/all]]
   [:div.d-md-none [sidebar-info-links]]])

(defn page []
  [personal-discussions-view])

(rf/reg-event-db
  :feed.sort/set
  (fn [db [_ method]]
    (assoc-in db [:feed :sort] method)))

(rf/reg-sub
  :feed/sort
  (fn [db _]
    (get-in db [:feed :sort] :time)))
