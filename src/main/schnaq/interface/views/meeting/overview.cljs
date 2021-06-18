(ns schnaq.interface.views.meeting.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.header-image :as header-image]
            [schnaq.interface.views.pages :as pages]))

(defn- no-meetings-found
  "Show error message when no meetings were loaded."
  []
  [:div.alert.alert-primary.text-center
   [:p.lead
    "🙈 "
    (labels :schnaqs.not-found/alert-lead)]
   [:p (labels :schnaqs.not-found/alert-body)]
   [:div.btn.btn-outline-primary
    {:on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
    (labels :nav.schnaqs/create-schnaq)]])


;; -----------------------------------------------------------------------------

(defn- schnaq-entry
  "Displays a single schnaq of the schnaq list"
  [schnaq]
  (let [share-hash (:discussion/share-hash schnaq)
        title (:discussion/title schnaq)
        url (header-image/check-for-header-img (:discussion/header-image-url schnaq))]
    [:div.meeting-entry
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.schnaq/start
                                {:share-hash share-hash}])
                  (rf/dispatch [:schnaq/select-current schnaq]))}
     [:div [:img.meeting-entry-title-header-image {:src url}]]
     [:div.meeting-entry-title
      [:h5 title]]]))

(defn- schnaq-list-view
  "Shows a list of schnaqs."
  [subscription-key]
  [:div.meetings-list
   (let [schnaqs @(rf/subscribe [subscription-key])]
     (if (empty? schnaqs)
       [no-meetings-found]
       (for [schnaq schnaqs]
         [:div.py-3 {:key (:db/id schnaq)}
          [schnaq-entry schnaq]])))])

(>defn- schnaq-overview
  "Shows the page for an overview of schnaqs. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  schnaqs."
  [subscription-key]
  [keyword? :ret vector?]
  [pages/with-nav-and-header
   {:page/heading (labels :schnaqs/header)
    :page/subheading (labels :schnaqs/subheader)}
   [:div.container.py-4
    [schnaq-list-view subscription-key]]])

(defn public-discussions-view
  "Render all public discussions."
  []
  [schnaq-overview :schnaqs/public])

(defn meeting-view-visited
  "Render visited meetings."
  []
  [schnaq-overview :schnaqs.visited/all])

;; #### Subs ####

(rf/reg-sub
  :schnaqs/all
  (fn [db _]
    (get-in db [:schnaqs :all])))
