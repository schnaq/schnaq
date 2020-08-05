(ns meetly.meeting.interface.views
  (:require [reagent.dom]
            [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.views.modals.modal :as modal]
            [re-frame.core :as rf]))

(defn- base-page
  []
  (let [current-route @(rf/subscribe [:current-route])
        errors @(rf/subscribe [:error-occurred])
        ajax-error (:ajax errors)]
    [:div#display-content
     ;[header]
     [:div#error-display.container
      (when ajax-error
        [:div.alert.alert-danger.alert-dismissible.fade.show "Error: " ajax-error])]
     (when current-route
       [:div
        [modal/modal]
        [(-> current-route :data :view)]])]))

(defn- footer []
  [base/footer])

(defn root []
  [:div#root
   [base-page]
   [footer]])
