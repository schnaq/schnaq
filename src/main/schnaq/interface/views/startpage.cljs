(ns schnaq.interface.views.startpage
  "Defining the startpage of schnaq."
  (:require [schnaq.interface.views.base :as base]
            [schnaq.interface.text.display-data :as data]
            [re-frame.core :as rf]))

(defn- header-animation
  "Display header animation video"
  []
  [:div.col-lg-6
   [:img.w-100 {:src (data/img-path :animation-discussion)}]])

(defn- header []
  [base/header
   (data/labels :start-page-subheader)
   (data/labels :start-page-subheader-2)
   [:div.pt-5 {:key "HeaderExtras-Bullet-Points-and-Animation"}
    [:div.row
     [:div.col-lg-6.icon-bullets
      (base/icon-bullet (data/img-path :icon-community) (data/labels :start-page-point-1))
      (base/icon-bullet (data/img-path :icon-robot) (data/labels :start-page-point-2))
      (base/icon-bullet (data/img-path :icon-reports) (data/labels :start-page-point-3))]
     [header-animation]]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.text-center
   [:button.btn.button-call-to-action
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
    (data/labels :create-schnaq-button)]])

(defn- under-construction
  []
  [:div.icon-bullets-larger
   (base/img-bullet-subtext (data/img-path :icon-crane)
                            (data/labels :start-page-point-alpha)
                            (data/labels :start-page-point-alpha-subtext))])

(defn- icons-grid
  "Display features in a grid."
  []
  [:section.features-icons.text-center
   [:div.container
    [:div.row
     (base/icon-in-grid (data/fa :laptop) (data/labels :innovative) (data/labels :innovative-why))
     (base/icon-in-grid (data/fa :comment) (data/labels :communicative) (data/labels :communicative-why))
     (base/icon-in-grid (data/fa :carry) (data/labels :cooperative) (data/labels :cooperative-why))]]])

(defn- usage-of-schnaq-heading
  "Heading introducing the features of schnaq."
  []
  [:div.d-flex.d-row.justify-content-center
   [:p.display-5 "Wofür kann ich schnaq verwenden?"]
   [:img.pl-3.d-md-none.d-lg-block
    {:style {:max-height "3rem"}
     :src (data/img-path :schnaqqifant/original)}]])

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
    [usage-of-schnaq-heading]]])

(defn startpage-view
  "A view that represents the first page of schnaq participation or creation."
  []
  [startpage-content])