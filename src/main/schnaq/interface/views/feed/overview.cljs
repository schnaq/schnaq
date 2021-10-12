(ns schnaq.interface.views.feed.overview
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.time :as util-time]
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
  [motion/fade-in-and-out
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
  (let [locale @(rf/subscribe [:current-locale])
        share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        time (:discussion/created-at schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:article.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div.d-flex.flex-row
      [:img.schnaq-header-image {:src url}]
      [:div.ml-3.w-100.py-2
       [:div.meeting-entry-title (toolbelt/truncate-to-n-chars title 40)]
       [:div.d-flex.flex-row.mt-auto.pt-3
        [user/user-info-only (:discussion/author schnaq) 24]
        [:div [badges/read-only-badge schnaq]]
        [:div [badges/static-info-badges schnaq]]
        [:small.font-weight-light.d-inline.my-auto.ml-auto
         [util-time/timestamp-with-tooltip time locale]]]]
      (when delete-from-hub?
        [:button.btn.btn-outline-dark.btn-small.my-auto.mr-3
         {:title (labels :hub.remove.schnaq/tooltip)
          :on-click (fn [e] (js-wrap/stop-propagation e)
                      (when (js/confirm (labels :hub.remove.schnaq/prompt))
                        (rf/dispatch [:hub.remove/schnaq share-hash])))}
         [icon :cross "m-auto"]])]]))

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
       [:div.panel-white.rounded-1
        [:div.d-flex.flex-row.mb-4
         [:h6.text-purple-dark.d-md-none.d-lg-block (labels :router/visited-schnaqs)]
         [:div.ml-auto
          [sort-options]
          [filters/filter-button]]]
        (for [schnaq sorted-schnaqs]
          [:div.pb-4 {:key (:db/id schnaq)}
           [schnaq-entry schnaq show-delete-from-hub-button?]])]))))

(defn- feed-button
  "Create a button for the feed list."
  ([text image-div class-button route]
   [feed-button text image-div class-button route nil])
  ([text image-div button-class route route-params]
   [:a.btn.btn-link.text-left {:class button-class
                               :role "button"
                               :href (reitfe/href route route-params)}
    [:div.d-flex.flex-row
     image-div
     [:div.pt-1.pl-2 text]]]))

(defn label-feed-button
  [label icon-name route route-params]
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        button-class (if (= current-route route) "feed-button-focused" "feed-button")
        icon-section (if icon-name [:div.mx-2.my-auto [icon icon-name "m-auto"]] nil)]
    [feed-button (labels label) icon-section button-class route route-params]))

(defn create-feed-button
  [label icon-name route route-params]
  (let [button-class "feed-button-create"
        icon-section (if icon-name [:div.mx-2.my-auto [icon icon-name "m-auto"]] nil)]
    [feed-button (labels label) icon-section button-class route route-params]))


(defn hub-feed-button
  "Display a single hub."
  [{:hub/keys [keycloak-name name logo]}]
  (let [current-hub @(rf/subscribe [:hub/current])
        current-hub-name (:hub/keycloak-name current-hub)
        path-keycloak-name keycloak-name
        button-class (if (= current-hub-name path-keycloak-name) "feed-button-focused" "feed-button")]
    [feed-button name [hub/hub-logo logo name 32] button-class :routes/hub {:keycloak-name keycloak-name}]))

(defn feed-hubs []
  (when-let [hubs @(rf/subscribe [:hubs/all])]
    [:section
     [:h6.text-purple-dark.pb-2.ml-4 (labels :hubs/heading)]
     [:div
      (for [[keycloak-name hub] hubs]
        (with-meta [hub-feed-button hub] {:key keycloak-name}))
      [label-feed-button :router/visited-schnaqs :eye :routes.schnaqs/personal]]]))

(defn feed-navigation
  "Navigate between the feeds."
  []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])
        hubs @(rf/subscribe [:hubs/all])]
    [:section
     (when hubs
       [:div.panel-white.mx-0.mt-0.mb-md-4
        [feed-hubs]])
     [:div.panel-white.m-0
      (when-not (nil? edit-hash)
        [label-feed-button :nav.schnaqs/last-added :arrow-left
         :routes.schnaq/admin-center {:share-hash share-hash :edit-hash edit-hash}])
      [create-feed-button :nav.schnaqs/create-schnaq :plus :routes.schnaq/create]]]))

(defn- outline-info-button
  "Generic outline button."
  [label href-link]
  [:article.w-100
   [:a.feed-button-outlined {:href href-link}
    (labels label)]])

(defn sidebar-info-links []
  [:section.panel-white.text-center
   [:div.btn-group {:role "group"}
    [:div.btn-group-vertical
     [outline-info-button :coc/heading (reitfe/href :routes/code-of-conduct)]
     [outline-info-button :how-to/button (reitfe/href :routes/how-to)]]]])

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
   [:div [sidebar-info-links]]])

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
