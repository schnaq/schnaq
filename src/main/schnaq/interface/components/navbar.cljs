(ns schnaq.interface.components.navbar
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn language-dropdown
  "Dropdown for bootstrap navbar to display the allowed languages."
  ([]
   [language-dropdown true {}])
  ([side-by-side? options]
   (let [icon-classes (if side-by-side? "" "d-block mx-auto")]
     [:<>
      [:button#schnaq-language-dropdown.btn.btn-link.nav-link.dropdown-toggle
       (merge
        {:role "button" :data-bs-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        options)
       [icon :language icon-classes {:size "lg"}]
       [:span.small " " @(rf/subscribe [:current-language])]]
      [:div.dropdown-menu {:aria-labelledby "schnaq-language-dropdown"}
       [:a.btn.dropdown-item
        {:href (navigation/switch-language-href :de)
         :lang "de-DE" :hrefLang "de-DE"}
        "Deutsch"]
       [:a.btn.dropdown-item
        {:href (navigation/switch-language-href :en)
         :lang "en-US" :hrefLang "en-US"}
        "English"]]])))

(defn language-toggle-with-tooltip
  "Uses language-dropdown and adds a mouse-over label."
  [side-by-side? options]
  [tooltip/text
   (labels :nav.buttons/language-toggle)
   [:span [language-dropdown side-by-side? options]]])

(>defn button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:a.nav-link.text-nowrap {:href href :role "button"}
   (labels label)])

(defn separated-button
  "The default navbar-button. Dropdown-content must have according classes."
  ([button-content]
   [separated-button button-content {}])
  ([button-content attributes]
   [separated-button button-content attributes nil])
  ([button-content attributes dropdown-content]
   [:<>
    [:button.btn.discussion-navbar-button.text-decoration-none
     (merge
      {:type "button"}
      attributes)
     button-content]
    dropdown-content]))

(defn collapsible-nav-bar
  "Collapsible navbar with split content header, collapsible-content-id must match id of collapsible-content."
  [brand-content collapse-content-id navbar-bg-class top-right-content collapsible-content]
  [:<>
   [:nav.navbar.navbar-expand-lg.navbar-light.schnaq-navbar-dynamic-padding
    {:class navbar-bg-class}
    [:div.container-fluid
     [:div.navbar-brand.p-0.btn brand-content]
     [:button.navbar-toggler.mx-2.panel-white
      {:type "button" :data-bs-toggle "collapse"
       :data-bs-target (str "#" collapse-content-id)
       :aria-controls collapse-content-id
       :aria-expanded "false"
       :aria-label "Toggle navigation"}
      [:span.navbar-toggler-icon]]
     [:div.d-md-none [common-components/theme-logo {:style {:max-width "100px"}}]]
     [:div.ms-auto.d-none.d-lg-block
      top-right-content]]]
   collapsible-content])

(>defn button-with-icon
  "Build a button for the navbar, with icon, text and tooltip."
  ([icon-key tooltip-text button-text on-click-fn]
   [keyword? string? string? fn? => :re-frame/component]
   [button-with-icon icon-key tooltip-text button-text on-click-fn nil])
  ([icon-key tooltip-text button-text on-click-fn attrs]
   [keyword? string? string? fn? (? map?) => :re-frame/component]
   [tooltip/tooltip-button "bottom" tooltip-text
    [:<>
     [icon icon-key "m-auto d-block" {:size "lg"}]
     [:span.small.text-nowrap button-text]]
    on-click-fn
    attrs]))
