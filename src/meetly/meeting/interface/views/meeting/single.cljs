(ns meetly.meeting.interface.views.meeting.single
  (:require [meetly.meeting.interface.views.agenda :as agenda-views]
            [re-frame.core :as rf]
            [meetly.meeting.interface.text.display-data :as data]
            [meetly.meeting.interface.views.base :as base]))


(defn- single-meeting []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [:h2 (str "foo" (:meeting/title current-meeting))]
     [:p (:meeting/description current-meeting)]
     [:hr]
     [agenda-views/agenda-in-meeting-view]]))

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  [:div
   [base/header-2
    (data/labels :meetings/header)]
   [:div.container.py-4
    [single-meeting]]])

(rf/reg-sub
  :selected-meeting
  (fn [db _]
    (get-in db [:meeting :selected])))