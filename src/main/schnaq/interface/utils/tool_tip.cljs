(ns schnaq.interface.utils.tool-tip
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn nested-div
  "Create a div with a tooltip, content is placed inside this div"
  [tooltip-location tooltip content]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [] [:div {:data-toggle "tooltip"
                  :data-placement tooltip-location
                  :title tooltip}
             content])}))