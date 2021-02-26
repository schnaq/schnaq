(ns schnaq.interface.utils.tooltip
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn nested-div
  "Create a div with a tooltip, content is placed inside this div"
  [_tooltip-location _tooltip _content]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [tooltip-location tooltip content]
       [:div.h-100 {:data-toggle "tooltip"
                    :data-placement tooltip-location
                    :title tooltip} content])}))

(defn tooltip-button
  [_tooltip-location _tooltip _content _on-click-fn]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [tooltip-location tooltip content on-click-fn]
       [:button.button-secondary-b-1.button-md.my-2.mx-3
        {:on-click on-click-fn
         :data-toggle "tooltip"
         :data-placement tooltip-location
         :title tooltip} content])}))