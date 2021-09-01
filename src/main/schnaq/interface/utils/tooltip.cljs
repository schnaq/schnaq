(ns schnaq.interface.utils.tooltip
  (:require ["@tippyjs/react" :default Tippy]
            [reagent.core :as reagent]))

(defn html
  "Wraps some content in a tooltip with the provided html inside."
  [tooltip-content wrapped-element options]
  [:> Tippy
   (merge
     {:animation "shift-away"
      :arrow true
      :content (reagent/as-element tooltip-content)
      :interactive true
      :offset [0 10]
      :placement "bottom"
      :theme "light"
      :trigger "click"}
     options)
   wrapped-element])

(defn text
  "Wraps some content in a tooltip with the provided text."
  [title content options]
  [:> Tippy
   (merge
     {:animation "shift-away"
      :arrow true
      :offset [0 10]
      :placement "bottom"
      :theme "light"
      :content title}
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
