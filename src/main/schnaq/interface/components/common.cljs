(ns schnaq.interface.components.common
  (:require [com.fulcrologic.guardrails.core :refer [>defn =>]]
            [schnaq.interface.components.icons :refer [icon]]))

(>defn hint-text
  "Info box to explain functionality."
  [text]
  [string? => :re-frame/component]
  [:small.text-muted.text-left
   [:div.d-flex.flex-row
    [icon :info "my-auto mr-3"]
    text]])

(defn pro-badge
  "Display a pro badge for pro users."
  []
  [:span.badge.rounded-pill.bg-light "pro"])
