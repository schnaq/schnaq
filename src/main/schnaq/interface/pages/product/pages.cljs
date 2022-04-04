(ns schnaq.interface.pages.product.pages
  (:require [schnaq.interface.pages.product.elements :as elements]))

(defn- product-tour []
  [elements/product-page
   :productpage.overview/heading
   :productpage.overview/subtitle
   :productpage.overview/title
   :productpage.overview/description
   :productpage.overview/cta-button
   nil
   [:<>
    [elements/feature-text-img-right
     :productpage.overview.qa/title
     :productpage.overview.qa/text
     :productpage.overview/qa
     [elements/find-out-more-link :routes/product-page-qa]]
    [elements/feature-text-img-left
     :productpage.overview.poll/title
     :productpage.overview.poll/text
     :productpage.overview/poll
     [elements/find-out-more-link :routes/product-page-poll]]
    [elements/feature-text-img-right
     :productpage.overview.activation/title
     :productpage.overview.activation/text
     :productpage.overview/activation
     [elements/find-out-more-link :routes/product-page-activation]]
    [elements/feature-text-img-left
     :productpage.overview.feedback/title
     :productpage.overview.feedback/text
     :productpage.overview/analysis
     [elements/available-soon]]]])

(defn overview-view
  "Product tour main page"
  []
  [product-tour])

(defn- product-qa []
  [elements/product-page
   :productpage.qa/heading
   :productpage.qa/subtitle
   :productpage.qa/title
   :productpage.qa/description
   :productpage.qa/cta-button
   :feature/free
   [:<>
    [elements/qa-feature-row]
    [elements/feature-text-img-left
     :productpage.qa.answers/title
     :productpage.qa.answers/subtitle
     :productpage.qa/answers]
    [elements/feature-text-img-right
     :productpage.qa.input/title
     :productpage.qa.input/subtitle
     :productpage.qa/input]
    [elements/feature-text-img-left
     :productpage.qa.relevant/title
     :productpage.qa.relevant/subtitle
     :productpage.qa/relevant]]])

(defn qa-view
  "Q&A product sub page."
  []
  [product-qa])

(defn- product-poll []
  [elements/product-page
   :productpage.poll/heading
   :productpage.poll/subtitle
   :productpage.poll/title
   :productpage.poll/description
   :productpage.poll/cta-button
   :feature/pro
   [:<>
    [elements/feature-text-img-right
     :productpage.poll-vote/title
     :productpage.poll-vote/subtitle
     :productpage.poll/select]
    [elements/feature-text-img-left
     :productpage.poll.single/title
     :productpage.poll.single/subtitle
     :productpage.poll/single]
    [elements/feature-text-img-right
     :productpage.poll.multiple/title
     :productpage.poll.multiple/subtitle
     :productpage.poll/multiple]]])

(defn poll-view
  "Poll product sub page."
  []
  [product-poll])

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

(defn theming-view
  "Theming product sub page"
  []
  [product-theming])
