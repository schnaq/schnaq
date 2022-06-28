(ns schnaq.interface.pages.product.pages
  (:require [schnaq.interface.pages.product.elements :as elements]))

(defn- product-activation []
  [elements/product-page
   :productpage.activation/heading
   :productpage.activation/subtitle
   :productpage.activation/title
   :productpage.activation/description
   :productpage.activation/cta-button
   :feature/pro
   [:<>
    [elements/feature-text-img-right
     :productpage.activation.torooo/title
     :productpage.activation.torooo/subtitle
     :productpage.activation/torooo]
    [elements/feature-text-img-left
     :productpage.activation.raise-hands/title
     :productpage.activation.raise-hands/subtitle
     :productpage.activation/raise-hands]
    [elements/feature-text-img-right
     :productpage.activation.audience/title
     :productpage.activation.audience/subtitle
     :productpage.activation/user-view]]])

(defn activation-view
  "Activation product sub page."
  []
  [product-activation])

(defn- product-theming []
  [elements/product-page
   :productpage.theming/heading
   :productpage.theming/subtitle
   :productpage.theming/title
   :productpage.theming/description
   :productpage.theming/cta-button
   :feature/pro
   [:<>
    [elements/feature-text-img-right
     :productpage.theming.brand-identity/title
     :productpage.theming.brand-identity/subtitle
     :productpage.theming/brand-identity]
    [elements/feature-text-img-left
     :productpage.theming.easy/title
     :productpage.theming.easy/subtitle
     :productpage.theming/easy]
    [elements/feature-text-img-right
     :productpage.theming.apply/title
     :productpage.theming.apply/subtitle
     :productpage.theming/apply]]])

(defn theming-view
  "Theming product sub page"
  []
  [product-theming])
