(ns schnaq.interface.views.product.overview
  (:require [schnaq.interface.views.product.elements :as elements]))

(defn- product-tour []
  [elements/product-page
   [:<>
    [elements/feature-text-img-right
     :productpage.overview.qa/title
     :productpage.overview.qa/text
     :productpage.overview/qa
     [elements/find-out-more-link :routes/product-page-qa]]
    [elements/feature-text-img-left
     :productpage.overview.poll/title
     :productpage.overview.poll/text
     :productpage.overview/poll]
    [elements/feature-text-img-right
     :productpage.overview.activation/title
     :productpage.overview.activation/text
     :productpage.overview/activation]
    [elements/feature-text-img-left
     :productpage.overview.feedback/title
     :productpage.overview.feedback/text
     :productpage.overview/analysis]]])

(defn overview-view []
  [product-tour])

(defn- product-qa []
  [elements/product-page
   [:<>]])

(defn qa-view []
  [product-qa])
