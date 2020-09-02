(ns schnaq.interface.views
  (:require [re-frame.core :as rf]
            [reagent.dom]
            [schnaq.interface.views.feedback :as feedback]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.modals.modal :as modal]
            [schnaq.interface.views.notifications :as notifications]))

(defn- base-page
  []
  (let [current-route @(rf/subscribe [:navigation/current-route])]
    [:div.base-wrapper
     (when current-route
       [:<>
        [modal/modal]
        [(-> current-route :data :view)]])]))

(defn- footer []
  [base/footer])

(defn root []
  [:div#root
   [base-page]
   [footer]
   [feedback/button]
   [notifications/view]])
