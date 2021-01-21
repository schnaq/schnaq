(ns schnaq.interface.views.spinner.spinner
  (:require [reagent.core :as reagent]))

(def ^:private role-id "loading-status")
(def ^:private spinner-is-loading? (reagent/atom false))

(defn set-spinner-loading! [is-loading?]
  (reset! spinner-is-loading? is-loading?))


(defn view [ & [start-spinning?]]
  (reagent/create-class
    {:display-name "Visualization of Discussion Graph"
     :reagent-render
     (fn [_this]
       (when @spinner-is-loading?
         [:div.spinner-styling
          [:div.spinner-border.text-primary {:role role-id}
           [:span.sr-only "Loading..."]]]))
     :component-did-mount
     (fn [_this]
       (when start-spinning?
         (set-spinner-loading! true)))
     :component-will-unmount
     (fn [_this]
       (set-spinner-loading! false))}))


