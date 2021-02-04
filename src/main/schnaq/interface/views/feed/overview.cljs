(ns schnaq.interface.views.feed.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.pages :as pages]))

(defn- no-schnaqs-found
  "Show error message when no meetings were loaded."
  []
  [:div.alert.alert-primary.text-center
   [:p.lead
    "🙈 "
    (labels :schnaqs.not-found/alert-lead)]
   [:p (labels :schnaqs.not-found/alert-body)]
   [:div.btn.btn-outline-primary
    {:on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
    (labels :nav.schnaqs/create-schnaq)]])

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq]
  (let [schnaq-share-hash (:discussion/share-hash schnaq)
        meeting-share-hash (:meeting/share-hash schnaq)
        schnaq-title (:discussion/title schnaq)
        meeting-title (:meeting/title schnaq)
        title (if schnaq-title schnaq-title meeting-title)
        share-hash (if schnaq-share-hash schnaq-share-hash meeting-share-hash)
        schnaq-url (:discussion/header-image-url schnaq)
        meeting-url (:meeting/header-image-url schnaq)
        url (if schnaq-url schnaq-url meeting-url)]
    [:div.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:meeting/select-current {:db/id (random-uuid)
                                                         :meeting/title title
                                                         :meeting/share-hash share-hash
                                                         :meeting/header-image-url (:discussion/header-image-url schnaq)}]))}
     [:div [:img.meeting-entry-title-header-image {:src (header-image/check-for-header-img url)}]]
     [:div.meeting-entry-title
      [:h5 title]]]))

(defn- schnaq-list-view
  "Shows a list of schnaqs."
  [subscription-key]
  [:div.meetings-list
   (let [schnaqs @(rf/subscribe [subscription-key])]
     (if (empty? schnaqs)
       [no-schnaqs-found]
       (for [schnaq schnaqs]
         [:div.py-3 {:key (:db/id schnaq)}
          [schnaq-entry schnaq]])))])

(defn- feed-button [label route]
  [:div
   [:button.feed-button
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate route])}
    [:h5 (labels label)]]])

(defn- feed-navigation []
  [:div
   [feed-button :router/my-schnaqs :routes.meetings/my-schnaqs]
   [feed-button :router/public-discussions :routes/public-discussions]])

(defn- feed-page [subscription-key]
  [:div.row.px-0.mx-0
   [:div.col-3.py-3
    [feed-navigation]]
   [:div.col-6.py-3.px-5
    [schnaq-list-view subscription-key]]
   [:div.col-3.py-3]])

(>defn- schnaq-overview
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  [subscription-key]
  [keyword? :ret vector?]
  [pages/with-nav
   {}
   [:div.container-fluid
    [feed-page subscription-key]]])

(defn public-discussions-view
  "Render all public discussions."
  []
  [schnaq-overview :schnaqs/public])

(defn personal-discussions-view
  "Render all discussions in which the user participated."
  []
  [schnaq-overview :meetings.visited/all])