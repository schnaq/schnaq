(ns schnaq.interface.components.navbar
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
            [re-frame.core :as rf]
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
      [:a#schnaq-language-dropdown.nav-link.dropdown-toggle
       (merge
        {:href "#" :role "button" :data-bs-toggle "dropdown"
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

(defn- drop-down-button-link [link label]
  [:a.dropdown-item {:href (navigation/href link)} (labels label)])

(defn product-dropdown-button
  "Product button containing all subpages in its dropdown content."
  []
  (let [dropdown-id "product-drop-down-id"]
    [:div.dropdown
     [:button.btn.text-white.dropdown-toggle
      {:id dropdown-id
       :href "#" :role "button" :data-bs-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      (labels :productpage/button)]
     [:div.dropdown-menu
      {:aria-labelledby dropdown-id}
      [drop-down-button-link :routes/product-page :router/product]
      [drop-down-button-link :routes/product-page-qa :router/product-qa]
      [drop-down-button-link :routes/product-page-poll :router/product-poll]
      [drop-down-button-link :routes/product-page-activation :router/product-activation]]]))

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
     [:navbar-brand.p-0 {:href "#"} brand-content]
     [:button.navbar-toggler.mx-2 {:type "button" :data-bs-toggle "collapse"
                                   :data-bs-target (str "#" collapse-content-id)
                                   :aria-controls collapse-content-id
                                   :aria-expanded "false"
                                   :aria-label "Toggle navigation"}
      [:span.navbar-toggler-icon]]
     [:div.ms-auto.d-none.d-lg-block
      top-right-content]]]
   collapsible-content])
