(ns meetly.interface.views
  (:require [reagent.dom]
            [meetly.interface.views.base :as base]
            [meetly.interface.views.modals.modal :as modal]
            [meetly.interface.views.feedback :as feedback]
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
   [footer]
   [feedback/button]])
