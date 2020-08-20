(ns meetly.interface.views
  (:require [meetly.interface.views.feedback :as feedback]
            [meetly.interface.views.base :as base]
            [meetly.interface.views.errors :as errors]
            [meetly.interface.views.modals.modal :as modal]
            [re-frame.core :as rf]
            [reagent.dom]))

(defn- base-page
  []
  (let [current-route @(rf/subscribe [:navigation/current-route])
        errors @(rf/subscribe [:error-occurred])
        ajax-error (:ajax errors)]
    [:div#display-content
     ;[header]
     [:div#error-display.container
      [errors/upper-error-box ajax-error]]
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
