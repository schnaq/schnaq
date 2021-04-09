(ns schnaq.interface.views.feed.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.hub.common :as hub]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]))

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

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:article.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div [:img.meeting-entry-title-header-image {:src url}]]
     [:div.px-4.d-flex
      [:div.meeting-entry-title
       [:h5 title]]
      [:div.ml-auto.mt-3
       [badges/read-only-badge schnaq]]]
     [:div.px-4
      [badges/static-info-badges schnaq]]]))

(defn schnaq-list-view
  "Shows a list of schnaqs."
  ([subscription-vector]
   [schnaq-list-view subscription-vector schnaq-entry])
  ([subscription-vector single-schnaq-component]
   [:div.meetings-list
    (let [schnaqs @(rf/subscribe subscription-vector)
          sort-method @(rf/subscribe [:feed/sort])
          sorted-schnaqs (if (= :alphabetical sort-method)
                           (sort-by #(first (:discussion/title %)) schnaqs)
                           (sort-by :db/txInstant > schnaqs))]
      (if (empty? schnaqs)
        [no-schnaqs-found]
        (for [schnaq sorted-schnaqs]
          [:div.pb-4 {:key (:db/id schnaq)}
           [single-schnaq-component schnaq]])))]))

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
     [:section.row
      [:div.list-group.col-6.col-md-12 {:role "group"}
       [feed-button :router/my-schnaqs :routes.schnaqs/personal]
       [feed-button :router/public-discussions :routes.schnaqs/public]
       (when-not (nil? edit-hash)
         [feed-button :nav.schnaqs/last-added
          :routes.schnaq/admin-center {:share-hash share-hash :edit-hash edit-hash}])
       (when-not toolbelt/production?
         [feed-button :nav.schnaqs/show-all :routes/schnaqs])
       [feed-button :nav.schnaqs/create-schnaq :routes.schnaq/create]]
      [:div.col-md-12.col-6
       [:hr.d-none.d-md-block]
       [hub/list-hubs-with-heading]]]
     [:hr.d-block.d-md-none]]))

(defn- generic-feed-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn sort-options
  "Displays the different sort options for feed elements."
  []
  (let [sort-method @(rf/subscribe [:feed/sort])]
    [:section.pl-2.text-right
     [:span.small.mb-0
      (labels :badges.sort/sort) [:br]
      [:button.btn.btn-outline-primary.btn-sm.mx-1
       {:class (when (= sort-method :time) "active")
        :on-click #(rf/dispatch [:feed.sort/set :time])}
       (labels :badges.sort/newest)]
      [:button.btn.btn-outline-primary.btn-sm
       {:class (when (= sort-method :alphabetical) "active")
        :on-click #(rf/dispatch [:feed.sort/set :alphabetical])}
       (labels :badges.sort/alphabetical)]]]))

(defn sidebar-common []
  [:section.text-center.my-3.text-center
   [:div.btn-group {:role "group"}
    [:div.btn-group-vertical
     [generic-feed-button :coc/heading (reitfe/href :routes/code-of-conduct)]
     [generic-feed-button :how-to/button (reitfe/href :routes/how-to)]]]])

(defn feed-controls []
  [:<>
   [:section.panel-white
    [sidebar-common]]
   [sort-options]])

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
