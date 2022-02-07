(ns schnaq.interface.views.product.pages
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.wavy :as wavy]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.views.pages :as pages]))

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.mt-5
   [buttons/anchor-big
    (labels :schnaq.startpage.cta/button)
    (rfe/href :routes.schnaq/create)
    "btn-dark d-inline-block"]])

(defn product-above-the-fold
  "Displays a list of features with a call-to-action button to start a schnaq"
  [title subtitle]
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.my-auto
    [:h1 (labels title)]
    [:p.lead (labels subtitle)]
    [start-schnaq-button]]
   [:div.col-lg-6.pb-4
    [:img.product-page-ipad {:src (img-path :productpage.overview/ipad)}]]])

(defn- feature-text [title text]
  [:<>
   [:div.display-4.text-primary.mb-5 (labels title)]
   [:div.display-6.text-typography (labels text)]])

(defn- feature-image [image]
  [:div
   [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
   [:img.product-page-feature-image.my-auto {:src (img-path image)}]])

(defn- feature-text-img-right [title text image]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6 [feature-text title text]]
   [:div.col-12.col-lg-6.mt-5.mt-lg-0 [:div.mr-lg-n5 [feature-image image]]]])

(defn- feature-text-img-left [title text image]
  [:div.row.py-5.mt-5
   [:div.col-12.col-lg-6.d-none.d-lg-block [:div.ml-lg-n5 [feature-image image]]]
   [:div.col-12.col-lg-6 [feature-text title text]]
   [:div.col-12.d-lg-none.mt-5 [feature-image image]]])

(defn- product-tour []
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title (labels :startpage/heading)
     :page/wrapper-classes "container container-85"
     :page/vertical-header? true
     :page/more-for-heading (with-meta [product-above-the-fold
                                        :productpage.overview/title
                                        :productpage.overview/subtitle]
                              {:key "unique-cta-key"})}
    [:div.product-background
     [:section.container.container-85
      [feature-text-img-right
       :productpage.overview.qa/title
       :productpage.overview.qa/text
       :productpage.overview/qa]
      [feature-text-img-left
       :productpage.overview.poll/title
       :productpage.overview.poll/text
       :productpage.overview/poll]
      [feature-text-img-right
       :productpage.overview.activation/title
       :productpage.overview.activation/text
       :productpage.overview/activation]
      [feature-text-img-left
       :productpage.overview.feedback/title
       :productpage.overview.feedback/text
       :productpage.overview/analysis]]
     [:div.wave-bottom-typography]]]])

(defn overview-view []
  [product-tour])