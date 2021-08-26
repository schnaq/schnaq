(ns schnaq.interface.utils.tooltip
  (:require ["react-tippy" :refer [Tooltip]]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [reagent.core :as reagent]))

(defn html
  "Wraps some content in a tooltip with the provided html inside."
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
  "Wraps some content in a tooltip with the provided text."
  [title content options]
  [:> Tooltip
   (merge
     {:animation "shift"
      :arrow true
      :offset 5
      :position "bottom"
      :theme "light"
      :title title
      :tag :span}
     options)
   content])

(defn tooltip-button
  [tooltip-location tooltip content on-click-fn]
  [text
   tooltip
   [:button.btn.btn-outline-muted.btn-lg
    {:on-click on-click-fn}
    content]
   {:position tooltip-location}])