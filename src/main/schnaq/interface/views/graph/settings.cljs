(ns schnaq.interface.views.graph.settings
  (:require [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn- gravity-slider
  "Show gravity slider."
  []
  (let [slider-id (gstring/format "gravity-slider-%s" (random-uuid))
        set-gravity! (fn [e]
                       (let [slider-value (js/parseInt (oget e [:target :value]))]
                         (rf/dispatch [:graph.settings/gravity! (/ slider-value 100)])))]
    [:div.form-group
     [:label {:for slider-id}
      (labels :graph.settings.gravity/label)]
     [:input.form-control-range.graph-settings-gravity
      {:id slider-id
       :on-input set-gravity!                               ;; For browser compatibility, set both events
       :on-change set-gravity!
       :min 0 :max 100
       :value (* 100 @(rf/subscribe [:graph.settings/gravity]))
       :type "range"}]]))

(defn- show-notification
  "Configure the gravity of the nodes."
  []
  (rf/dispatch
    [:notification/add
     #:notification{:title (labels :graph.settings/title)
                    :body [:<>
                           [:p (labels :graph.settings/description)]
                           [:hr]
                           [gravity-slider]]
                    :context :info
                    :stay-visible? true}]))


;; -----------------------------------------------------------------------------

(defn open-settings
  "Open Settings for the graph."
  []
  [tooltip/tooltip-button "bottom" (labels :graph.settings/title)
   [:i {:class "fas fa-sliders-h"}]
   show-notification])