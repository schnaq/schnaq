(ns schnaq.interface.views.meeting.overview
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.text.display-data :as data :refer [labels]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]))

(defn- readable-date [date]
  (when date
    [:span (str (.toLocaleDateString date (language/locale)))]))

(defn- no-meetings-found
  "Show error message when no meetings were loaded."
  []
  [:div.alert.alert-primary.text-center
   [:p.lead
    "🙈 "
    (labels :schnaqs.not-found/alert-lead)]
   [:p (labels :schnaqs.not-found/alert-body)]
   [:div.btn.btn-outline-primary
    {:on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
    (labels :nav-meeting-create)]])


;; -----------------------------------------------------------------------------

(defn- meeting-entry
  "Displays a single meeting element of the meeting list"
  [meeting]
  ;; clickable div
  [:div.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigation/navigate :routes.meeting/show
                              {:share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:meeting/select-current meeting]))}
   ;; title and arrow
   [:div.meeting-entry-title
    [:div.row
     [:div.col-lg-11.px-3
      [:h3 (:meeting/title meeting)]]
     [:div.col-lg-1
      [:i.arrow-icon {:class (str "m-auto fas " (data/fa :arrow-right))}]]]]
   ;; description / body
   [:div.meeting-entry-desc
    [:hr]
    [:div (data/labels :meeting-form-deadline) ": " [readable-date (:meeting/end-date meeting)]]
    [:small.text-right.float-right
     (common/avatar (-> meeting :meeting/author :author/nickname) 50)]
    [:br]
    [:p (:meeting/description meeting)]]])

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
  [:<>
   [base/nav-header]
   [base/header
    (data/labels :meetings/header)]
   [:div.container.py-4
    [meetings-list-view subscription-key]]])

(defn meeting-view-entry
  "Render all meetings."
  []
  [meeting-view :meetings/all])

(defn meeting-view-visited
  "Render visited meetings."
  []
  [meeting-view :meetings.visited/all])

;; #### Subs ####

(rf/reg-sub
  :meetings/all
  (fn [db _]
    (get-in db [:meetings :all])))
