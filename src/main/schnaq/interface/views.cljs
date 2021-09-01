(ns schnaq.interface.views
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.notifications :as notifications]))

(defn- base-page
  [language]
  (let [current-route @(rf/subscribe [:navigation/current-route])]
    [:div.base-wrapper {:key language}
     (when current-route
       [:<>
        [modal/modal-view]
        (if-let [current-view (-> current-route :data :view)]
          [current-view]
          [:div])])]))

(defn- footer []
  [base/footer])

(defn root []
  (let [language @(rf/subscribe [:current-locale])]
    [:div#root {:key language}
     [base-page language]
     (when-not shared-config/embedded?
       [footer])
     [notifications/view]]))
