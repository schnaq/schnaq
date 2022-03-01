(ns schnaq.interface.components.common
  (:require [com.fulcrologic.guardrails.core :refer [>defn =>]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]))

(>defn hint-text
  "Info box to explain functionality."
  [text]
  [string? => :re-frame/component]
  [:small.text-muted.text-start
   [:div.d-flex.flex-row
    [icon :info "my-auto me-3"]
    text]])

(defn pro-badge
  "Display a pro badge for pro users."
  []
  [:span.badge.rounded-pill.bg-primary "pro"])

(defn theme-logo
  "Show the current logo configured in a theme."
  []
  (let [theme @(rf/subscribe [:schnaq.selected/theme])]
    [:img.theme-logo.img-fluid.px-2
     {:src (:theme.images/logo theme)}]))