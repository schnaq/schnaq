(ns schnaq.interface.views.feed.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user :as user]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [common/delayed-fade-in
   [:div.alert.alert-primary.text-center
    [:p.lead
     "🙈 "
     (labels :schnaqs.not-found/alert-lead)]
    [:p (labels :schnaqs.not-found/alert-body)]
    [:button.btn.btn-outline-primary
     {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
     (labels :nav.schnaqs/create-schnaq)]]])

(defn sort-options
  "Displays the different sort options for feed elements."
  []
  (let [sort-method @(rf/subscribe [:feed/sort])]
    [:section.pl-2.text-right
     [:span.small
      [:button.btn.btn-outline-primary.btn-sm.mx-1
       {:class (when (= sort-method :time) "active")
        :on-click #(rf/dispatch [:feed.sort/set :time])}
       (labels :badges.sort/newest)]
      [:button.btn.btn-outline-primary.btn-sm
       {:class (when (= sort-method :alphabetical) "active")
        :on-click #(rf/dispatch [:feed.sort/set :alphabetical])}
       (labels :badges.sort/alphabetical)]]]))

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
       [:img.meeting-entry-title-header-image {:src url}]]
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
   [:div.meetings-list
    (let [schnaqs @(rf/subscribe subscription-vector)
          sort-method @(rf/subscribe [:feed/sort])
          sorted-schnaqs (if (= :alphabetical sort-method)
                           (sort-by :discussion/title schnaqs)
                           (sort-by :discussion/created-at > schnaqs))]
      (if (empty? schnaqs)
        [no-schnaqs-found]
        [:div.panel-white.rounded-1.px-md-5
         [:div.row.pl-5
          [:div.col-3.col-md-5 [:p.text-muted (labels :schnaqs/author)]]
          [:div.col-2.col-md-2 [:p.text-muted (labels :schnaqs/schnaq)]]
          [:div.col-7.col-md-5 [sort-options]]]
         (for [schnaq sorted-schnaqs]
           [:div.pb-4 {:key (:db/id schnaq)}
            [schnaq-entry schnaq show-delete-from-hub-button?]])]))]))

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

(defn feed-navigation
  "Navigate between the feeds."
  []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])]
    [:<>
     [:section.px-3
      [:div.row.panel-white
       [:div.col-md-12.col-6.list-group
        [hub/list-hubs-with-heading]
        [:hr.d-none.d-md-block]]
       [:div.col-md-12.col-6
        [feed-button :router/visited-schnaqs :routes.schnaqs/personal]
        (when-not (nil? edit-hash)
          [feed-button :nav.schnaqs/last-added
           :routes.schnaq/admin-center {:share-hash share-hash :edit-hash edit-hash}])
        [feed-button :nav.schnaqs/create-schnaq :routes.schnaq/create]]]]]))

(defn- generic-feed-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn sidebar-common []
  [:section.text-center.text-center.panel-white
   [:div.btn-group {:role "group"}
    [:div.btn-group-vertical
     [generic-feed-button :coc/heading (reitfe/href :routes/code-of-conduct)]
     [generic-feed-button :how-to/button (reitfe/href :routes/how-to)]]]])

(defn feed-controls []
  [:<>
   [:section
    [sidebar-common]]])

(>defn- schnaq-overview
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  [subscription-vector page-header]
  [keyword? keyword? :ret vector?]
  [pages/three-column-layout
   {:page/heading (labels page-header)
    :page/subheading (labels :schnaqs/subheader)}
   [feed-navigation]
   [schnaq-list-view subscription-vector]
   [feed-controls]])

(defn public-discussions-view
  "Render all public discussions."
  []
  [schnaq-overview [:schnaqs/public] :schnaqs.all/header])

(defn personal-discussions-view
  "Render all discussions in which the user participated."
  []
  [schnaq-overview [:schnaqs.visited/all] :schnaqs/header])

(rf/reg-event-db
  :feed.sort/set
  (fn [db [_ method]]
    (assoc-in db [:feed :sort] method)))

(rf/reg-sub
  :feed/sort
  (fn [db _]
    (get-in db [:feed :sort] :time)))
