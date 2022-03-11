(ns schnaq.interface.components.common
  (:require [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon icon-card]]
            [schnaq.interface.navigation :as navigation]))

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
  [attrs]
  (let [{:theme.images/keys [logo]} @(rf/subscribe [:schnaq.selected/theme])]
    (when logo
      [:img.theme-logo
       (merge
        {:src logo
         :alt "Theme Logo"}
        attrs)])))

(>defn next-step
  "Show next possible steps, with a heading, lead text and a CTA."
  ([icon title body button-text route-name]
   [keyword? string? string? string? keyword? => :re-frame/component]
   [next-step icon title body button-text route-name false])
  ([icon title body button-text route-name disabled?]
   [keyword? string? string? string? keyword? (? boolean?) => :re-frame/component]
   (let [href (navigation/href route-name)]
     [:article.pb-3.pe-3
      [:a {:href href} [icon-card icon "text-typography" {:size :lg}]]
      [:p.fw-bold.my-2 title]
      [:p body]
      (when-not disabled?
        [buttons/anchor button-text href :btn-white])])))
