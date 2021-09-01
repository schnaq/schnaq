(ns schnaq.interface.utils.tooltip
  (:require ["@tippyjs/react" :default Tippy]
            ["react-tippy" :refer [Tooltip]]
            [reagent.core :as reagent]))

(defn html
  "Wraps some content in a tooltip with the provided html inside."
  [tooltip-content wrapped-element options]
  [:> Tooltip
   (merge
     {:animation "scale"
      :arrow true
      :html (reagent/as-element tooltip-content)
      :interactive true
      :offset 5
      :position "bottom"
      :theme "light"
      :trigger "click"}
     options)
   wrapped-element])

(defn text
  "Wraps some content in a tooltip with the provided text."
  [title content options]
  [:> Tippy
   (merge
     {:animation "shift"
      :arrow true
      :offset 5
      :position "bottom"
      :theme "light"
      :content (str title)
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
