(ns schnaq.interface.views.meeting.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
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
    {:on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
    (labels :nav.schnaqs/create-brainstorm)]])


;; -----------------------------------------------------------------------------

(defn- meeting-entry
  "Displays a single meeting element of the meeting list"
  [meeting]
  ;; clickable div
  [:div.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigation/navigate :routes.schnaq/start
                              {:share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:meeting/select-current meeting]))}
   ;; title and arrow
   [:div.meeting-entry-title
    [:div.row
     [:div.col-lg-11.px-3
      [:h3 (:meeting/title meeting)]]
     [:div.col-lg-1
      [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-right))}]]]]])

(defn- meetings-list-view
  "Shows a list of all meetings."
  [subscription-key]
  [:div.meetings-list
   (let [meetings @(rf/subscribe [subscription-key])]
     (if (empty? meetings)
       [no-meetings-found]
       (for [meeting meetings]
         [:div.py-3 {:key (:db/id meeting)}
          [meeting-entry meeting]])))])

(>defn- meeting-view
  "Shows the page for an overview of meetings. Takes a subscription-key which
  must be a keyword referring to a subscription, which returns a collection of
  meetings."
  [subscription-key]
  [keyword? :ret vector?]
  [pages/with-nav-and-header
   {:page/heading (labels :meetings/header)
    :page/subheading (labels :meetings/subheader)}
   [:div.container.py-4
    [meetings-list-view subscription-key]]])

(defn meeting-view-entry
  "Render all meetings."
  []
  [meeting-view :meetings/all])

(defn public-discussions-view
  "Render all public discussions."
  []
  [meeting-view :meetings/public])

(defn meeting-view-visited
  "Render visited meetings."
  []
  [meeting-view :meetings.visited/all])

;; #### Subs ####

(rf/reg-sub
  :meetings/all
  (fn [db _]
    (get-in db [:meetings :all])))
