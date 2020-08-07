(ns meetly.meeting.interface.views.meeting.overview
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.utils.language :as language]
            [meetly.meeting.interface.text.display-data :as data]
            [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.views.common :as common]))


(defn- readable-date [date]
  (when date
    [:span (str (.toLocaleDateString date (language/locale)))]))

(defn- meeting-entry
  "Displays a single meeting element of the meeting list"
  [meeting]
  ;; clickable div
  [:div.meeting-entry
   {:on-click (fn []
                (rf/dispatch [:navigate :routes/meetings.show
                              {:share-hash (:meeting/share-hash meeting)}])
                (rf/dispatch [:select-current-meeting meeting]))}
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
  []
  [:div.meetings-list
   (let [meetings @(rf/subscribe [:meetings])]
     (for [meeting meetings]
       [:div.py-3 {:key (:db/id meeting)}
        [meeting-entry meeting]]))])


(defn meeting-view
  "Shows the page for an overview of all meetings"
  []
  [:div
   [base/nav-header]
   [base/header
    (data/labels :meetings/header)]
   [:div.container.py-4
    [meetings-list-view]]])

;; #### Subs ####

(rf/reg-sub
  :meetings
  (fn [db _]
    (:meetings db)))
