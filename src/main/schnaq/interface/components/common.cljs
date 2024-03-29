(ns schnaq.interface.components.common
  (:require [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [goog.string :refer [format]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon icon-card]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.utils.tooltip :as tooltip]))

(>defn hint-text
  "Info box to explain functionality."
  [text]
  [string? => :re-frame/component]
  [:small.text-muted
   [icon :info "my-auto me-2"]
   text])

(>defn info-icon-with-tooltip
  "Display an info icon with a tooltip on mouse-over."
  [label attrs]
  [(s/or :string string? :component :re-frame/component) (? map?) => :re-frame/component]
  [tooltip/text
   label
   [:span attrs [icon :info-question "small ms-1" {:style {:cursor :help}}]]])

;; -----------------------------------------------------------------------------

(defn- badge-builder
  [props child]
  [:span.badge.rounded-pill.bg-gradient props child])

(defn admin-badge
  "Display an admin badge."
  [props]
  [badge-builder (merge {:class "bg-danger"} props) "admin"])

(defn analytics-admin-badge
  "Display an analytics-admin badge."
  [props]
  [badge-builder (merge {:class "bg-secondary"} props) "analytics"])

(defn tester-badge
  "Display a tester badge."
  [props]
  [badge-builder (merge {:class "bg-success"} props) "tester"])

(defn enterprise-badge
  "Display an enterprise badge."
  [props]
  [badge-builder (merge {:class "bg-info"} props) "enterprise"])

(defn pro-badge
  "Display a pro badge."
  [props]
  [badge-builder (merge {:class "bg-primary"} props) "pro"])

(defn free-badge
  "Display a free badge."
  [props]
  [badge-builder (merge {:class "bg-white"} props) "free"])

(defn role-indicator
  "Show an icon if the user has special roles."
  ([]
   [role-indicator false])
  ([with-free-badge?]
   (let [admin? @(rf/subscribe [:user/administrator?])
         analytics-admin? @(rf/subscribe [:user/analytics-admin?])
         beta-tester? @(rf/subscribe [:user/beta-tester?])
         pro-user? @(rf/subscribe [:user/pro?])
         enterprise-user? @(rf/subscribe [:user/enterprise?])
         indicator (cond
                     admin? [admin-badge]
                     analytics-admin? [analytics-admin-badge]
                     (and enterprise-user? (not beta-tester?)) [enterprise-badge]
                     (and pro-user? (not beta-tester?)) [pro-badge]
                     beta-tester? [tester-badge]
                     with-free-badge? [free-badge])]
     (when indicator
       [:span.px-1 indicator]))))

;; -----------------------------------------------------------------------------

(defn outlined-pill
  "Create an outlined badge ()rounded pill)."
  [content variant]
  (let [stringed-variant (str (name variant))]
    [:span.badge.rounded-pill.border.mx-1
     {:class (format "border-%s text-%s" stringed-variant stringed-variant)}
     content]))

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

(>defn schnaq-logo
  "Display a schnaq logo."
  [attrs]
  [(? map?) => :re-frame/component]
  [:img (merge
         {:src (img-path :logo)
          :alt "schnaq logo"}
         attrs)])

(defn schnaq-logo-white
  "Display a white schnaq logo."
  [& {:keys [props]}]
  [:img (merge
         {:src (img-path :logo-white)
          :alt "schnaq logo"}
         props)])

(defn schnaqqi-white
  "Display a white schnaqqi."
  [& {:keys [props]}]
  [:img (merge
         {:src (img-path :schnaqqifant/white)
          :alt "Image of schnaqqi"}
         props)])
