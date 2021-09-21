(ns schnaq.interface.components.navbar
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [fa]]
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
       [:i {:class (str "fas " (fa :language))}]
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
