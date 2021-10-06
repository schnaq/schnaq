(ns schnaq.interface.components.navbar
  (:require [ghostwheel.core :refer [>defn]]
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

(>defn navbar-button
  "Build a button for the navbar. Takes a label as a keyword and anything, which
  can be passed to an anchor's href."
  [label href]
  [keyword? any? :ret vector?]
  [:a.nav-link {:href href :role "button"}
   (labels label)])
