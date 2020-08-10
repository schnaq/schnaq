(ns meetly.meeting.interface.analytics.core
  (:require [meetly.meeting.interface.views.base :as base]))

(defn analytics-dashboard-view
  "The dashboard displaying all analytics."
  []
  [:div
   [base/nav-header]
   [:div.container.px-5.py-3
    "Hallo, hier die Analytics."]])