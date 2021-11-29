(ns schnaq.interface.views
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :as config]
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
    [:main#root.text-break {:key language}
     [base-page language]
     (when-not (or shared-config/embedded? config/in-iframe?)
       [footer])
     [notifications/view]]))
