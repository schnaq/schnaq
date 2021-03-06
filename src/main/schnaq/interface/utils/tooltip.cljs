(ns schnaq.interface.utils.tooltip
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(s/def :tooltip/placement #{:top :right :bottom :left})

(>defn- tooltip-builder
  [_wrapping-html _tooltip-placement _tooltip-text _content _on-click-fn]
  [keyword? :tooltip/placement string? any? fn? :ret fn?]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [wrapping-html tooltip-placement tooltip-text content on-click-fn]
       [wrapping-html
        {:on-click on-click-fn
         :data-toggle "tooltip"
         :data-placement tooltip-placement
         :title tooltip-text}
        content])}))

(defn block-element
  "Wrap the `content` with in an block-element and add a tooltip."
  [tooltip-location tooltip content]
  [tooltip-builder :div tooltip-location tooltip content nil])

(defn inline-element
  "Wrap the `content` with in an inline-element and add a tooltip."
  [tooltip-location tooltip content]
  [tooltip-builder :span tooltip-location tooltip content nil])

(defn tooltip-button
  [tooltip-location tooltip content on-click-fn]
  [tooltip-builder
   :button.button-secondary-b-1.button-md.my-2.mx-3
   tooltip-location tooltip content on-click-fn])