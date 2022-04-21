(ns schnaq.interface.views
  (:require [re-frame.core :as rf]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.notifications :as notifications]))

(defn- base-page []
  (let [language @(rf/subscribe [:current-locale])
        current-view @(rf/subscribe [:navigation/current-view])]
    [:div.base-wrapper {:key language}
     (when current-view
       [:<>
        [modal/modal-view]
        [current-view]])]))

(defn root []
  (let [language @(rf/subscribe [:current-locale])]
    [:main#root.text-break {:key language}
     [base-page]
     [notifications/view]]))
