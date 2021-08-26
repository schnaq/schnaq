(ns schnaq.interface.utils.tooltip
  (:require ["react-tippy" :refer [Tooltip]]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(s/def :tooltip/placement #{:top :right :bottom :left})

(defn html
  [component content options]
  [:> Tooltip
   (merge
     {:animation "scale"
      :arrow true
      :html (reagent/as-element component)
      :interactive true
      :offset 5
      :position "bottom"
      :theme "light"
      :trigger "click"}
     options)
   content])

(defn text
  [title content options]
  [:> Tooltip
   (merge
     {:animation "scale"
      :arrow true
      :offset 5
      :position "bottom"
      :theme "light"
      :title title}
     options)
   content])

(>defn- tooltip-builder
  [wrapping-html tooltip-placement tooltip-text content on-click-fn]
  [keyword? :tooltip/placement string? any? fn? :ret fn?]
  [text
   tooltip-text
   [wrapping-html
    {:on-click on-click-fn}
    content]
   {:position tooltip-placement}])

(defn block-element
  "Wrap the `content` with in an block-element and add a tooltip."
  [tooltip-location tooltip content]
  [tooltip-builder :div tooltip-location tooltip content])

(defn inline-element
  "Wrap the `content` with in an inline-element and add a tooltip."
  [tooltip-location tooltip content]
  [tooltip-builder :span tooltip-location tooltip content])

(defn tooltip-button
  [tooltip-location tooltip content on-click-fn]
  [tooltip-builder
   :button.btn.btn-outline-muted.btn-lg
   tooltip-location tooltip content on-click-fn])