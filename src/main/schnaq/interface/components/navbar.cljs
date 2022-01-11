(ns schnaq.interface.components.navbar
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.utils.tooltip :as tooltip]))

(defn language-dropdown
  "Dropdown for bootstrap navbar to display the allowed languages."
  ([]
   [language-dropdown true {}])
  ([show-label? options]
   (let [current-language @(rf/subscribe [:current-language])]
     [:<>
      [:a#schnaq-language-dropdown.nav-link.dropdown-toggle
       (merge
        {:href "#" :role "button" :data-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        options)
       [icon :language]
       (when show-label?
         (str " " current-language))]
      [:div.dropdown-menu {:aria-labelledby "schnaq-language-dropdown"}
       [:button.dropdown-item
        {:on-click #(language/set-language :de)} "Deutsch"]
       [:button.dropdown-item
        {:on-click #(language/set-language :en)} "English"]
       [:button.dropdown-item
        {:on-click #(language/set-language :pl)} "Polski"]]])))

(defn language-toggle-with-tooltip
  "Uses language-dropdown and adds a mouse-over label."
  [show-label? options]
  [tooltip/text
   (labels :nav.buttons/language-toggle)
   [:span [language-dropdown show-label? options]]])

(>defn button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:a.nav-link {:href href :role "button"}
   (labels label)])

(defn separated-button
  "The default navbar-button. Dropdown-content must have according classes."
  ([button-content]
   [separated-button button-content {}])
  ([button-content attributes]
   [separated-button button-content attributes nil])
  ([button-content attributes dropdown-content]
   [:<>
    [:button.btn.discussion-navbar-button
     (merge
      {:type "button"}
      attributes)
     button-content]
    dropdown-content]))

(defn collapsable-nav-bar
  "Collapsable navbar with split content header, collapsable-content-id must match id of collapsable-content."
  [brand-content collapse-content-id navbar-bg-class top-right-content collapsable-content]
  [:<>
   [:nav.navbar.navbar-expand-lg.navbar-light.schnaq-navbar-dynamic-padding
    {:class navbar-bg-class}
    [:navbar-brand.p-0 {:href "#"} brand-content]
    [:button.navbar-toggler.mx-2 {:type "button" :data-toggle "collapse"
                                  :data-target (str "#" collapse-content-id)
                                  :aria-controls collapse-content-id
                                  :aria-expanded "false"
                                  :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon]]
    [:div.ml-auto.d-none.d-lg-block
     top-right-content]]
   collapsable-content])
