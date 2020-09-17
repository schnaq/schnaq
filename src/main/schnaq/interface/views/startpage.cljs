(ns schnaq.interface.views.startpage
  "Defining the startpage of schnaq."
  (:require [schnaq.interface.views.base :as base]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [re-frame.core :as rf]))

(defn- header-animation
  "Display header animation video"
  []
  [:div.col-lg-6
   [:img.w-100 {:src (img-path :animation-discussion)}]])

(defn- header []
  [base/header
   (labels :startpage/heading)
   (labels :startpage/subheading)
   [:div.pt-5 {:key "HeaderExtras-Bullet-Points-and-Animation"}
    [:div.row
     [:div.col-lg-6.icon-bullets
      (base/icon-bullet (img-path :icon-community) (labels :startpage.heading-list/community))
      (base/icon-bullet (img-path :icon-robot) (labels :startpage.heading-list/exchange))
      (base/icon-bullet (img-path :icon-reports) (labels :startpage.heading-list/reports))]
     [header-animation]]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.text-center
   [:button.btn.button-call-to-action
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
    (labels :startpage.button/create-schnaq)]])

(defn- under-construction
  []
  [:div.icon-bullets-larger
   (base/img-bullet-subtext (img-path :icon-crane)
                            (labels :startpage.under-construction/heading)
                            (labels :startpage.under-construction/body))])

(defn- icons-grid
  "Display features in a grid."
  []
  [:section.features-icons.text-center
   [:div.container
    [:div.row
     (base/icon-in-grid (fa :laptop) (labels :startpage.grid/innovative) (labels :startpage.grid/innovative-body))
     (base/icon-in-grid (fa :comment) (labels :startpage.grid/communicative) (labels :startpage.grid/communicative-body))
     (base/icon-in-grid (fa :carry) (labels :startpage.grid/cooperative) (labels :startpage.grid/cooperative-body))]]])

(defn- usage-of-schnaq-heading
  "Heading introducing the features of schnaq."
  []
  [:div.d-flex.d-row.justify-content-center
   [:p.display-5 (labels :startpage.usage/lead)]
   [:img.pl-3.d-md-none.d-lg-block
    {:style {:max-height "3rem"}
     :src (img-path :schnaqqifant/original)}]])

(defn- build-feature-text-box
  "Composing the text-part of a feature-row."
  [lead title body]
  [:article.feature-text-box.pb-5
   [:p.lead.mb-1 lead]
   [:h5 title]
   [:p body]
   [:div.btn.btn-outline-dark
    (labels :startpage.features/more-information)]])

(defn- feature-row-image-left
  "Build a feature row, where the image is located on the left side."
  [image-key lead title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [:img.img-fluid {:src (img-path :startpage.features/sample-discussion)}]]
   [:div.col-12.col-lg-6.offset-lg-1
    [build-feature-text-box
     (labels :startpage.features.discussion/lead)
     (labels :startpage.features.discussion/title)
     (labels :startpage.features.discussion/body)]]])



(defn- feature-meeting-organisation
  "Featuring meeting-organisation with an image."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [build-feature-text-box
     (labels :startpage.features.meeting-organisation/lead)
     (labels :startpage.features.meeting-organisation/title)
     (labels :startpage.features.meeting-organisation/body)]]
   [:div.col-12.col-lg-5.offset-lg-1
    [:img.img-fluid {:src (img-path :startpage.features/meeting-organisation)}]]])

(defn- feature-structured-discussions
  "Overview of structured discussions."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [:img.img-fluid {:src (img-path :startpage.features/sample-discussion)}]]
   [:div.col-12.col-lg-6.offset-lg-1
    [build-feature-text-box
     (labels :startpage.features.discussion/lead)
     (labels :startpage.features.discussion/title)
     (labels :startpage.features.discussion/body)]]])

(defn- feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [feature-meeting-organisation]
   [feature-structured-discussions]])

(defn- startpage-content []
  [:<>
   [base/nav-header]
   [header]
   [:section.container
    [:div.row.mt-5
     [:div.col-12.col-lg-6.pb-3.pb-lg-0
      [under-construction]]
     [:div.col-12.col-lg-6
      [start-schnaq-button]]]
    [icons-grid]
    [usage-of-schnaq-heading]
    [feature-rows]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])