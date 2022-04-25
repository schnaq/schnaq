(ns schnaq.interface.utils.tooltip
  (:require ["@tippyjs/react" :default Tippy]
            [reagent.core :as reagent]))

(defn html
  "Wraps some content in a tooltip with the provided html inside."
  ([tooltip-content wrapped-element options]
   [html tooltip-content wrapped-element options nil])
  ([tooltip-content wrapped-element options deactivated-options]
   [:> Tippy
    (apply dissoc
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
           deactivated-options)
    wrapped-element]))

(defn text
  "Wraps content in a tooltip with the provided text. If you pass a react-
  component as `content`, wrap it in a `span` to enable forward referencing."
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
  "Create a button with a tooltip on hover."
  ([tooltip-location tooltip content on-click-fn]
   [tooltip-button tooltip-location tooltip content on-click-fn nil])
  ([tooltip-location tooltip content on-click-fn attrs]
   [text
    tooltip
    [:button.btn.btn-outline-muted
     (merge {:on-click on-click-fn} attrs)
     content]
    {:placement tooltip-location}]))
