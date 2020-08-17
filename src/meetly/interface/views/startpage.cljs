(ns meetly.interface.views.startpage
  (:require [meetly.interface.views.base :as base]
            [meetly.interface.text.display-data :as data]
            [re-frame.core :as rf]))


(defn- header-animation
  "Display header animation video"
  []
  [:div.col-lg-6
   [:img#animation_container {:src (data/img-path :animation-discussion)}]])


(defn- header []
  (base/header
    (data/labels :start-page-subheader)
    (data/labels :start-page-subheader-2)
    [:div.pt-5 {:key "HeaderExtras-Bullet-Points-and-Animation"}
     [:div.row
      [:div.col-lg-6.icon-bullets
       (base/icon-bullet (data/img-path :icon-community) (data/labels :start-page-point-community))
       (base/icon-bullet (data/img-path :icon-robot) (data/labels :start-page-point-moderation))
       (base/icon-bullet (data/img-path :icon-reports) (data/labels :start-page-point-reports))]
      [header-animation]]]))


(defn- call-to-action
  " Tell user to create a meetly now "
  []
  [:section
   [:div.container
    [:div.text-center
     [:div.mx-auto.col-lg-10
      [:button.btn.button-call-to-action
       {:type "button"
        :on-click #(rf/dispatch [:navigate :routes.meeting/create])}
       (data/labels :create-meetly-button)]
      [:p.pt-4 (data/labels :create-your-meeting-sub)]]]]])

(defn- under-construction
  []
  [:div.mt-5
   [:div.row
    [:div.col-lg-3]
    [:div.col-lg-6.icon-bullets-larger
     (base/img-bullet-subtext (data/img-path :icon-crane)
                              (data/labels :start-page-point-alpha)
                              (data/labels :start-page-point-alpha-subtext))]]])

(defn- icons-grid
  " Display features in a grid "
  []
  [:section.features-icons.text-center
   [:div.container
    [:div.row
     (base/icon-in-grid (data/fa :laptop) (data/labels :innovative) (data/labels :innovative-why))
     (base/icon-in-grid (data/fa :comment) (data/labels :communicative) (data/labels :communicative-why))
     (base/icon-in-grid (data/fa :carry) (data/labels :cooperative) (data/labels :cooperative-why))]]])


(defn- startpage-content []
  [:div
   [base/nav-header]
   [header]
   [:div.container
    [call-to-action]
    [under-construction]
    [icons-grid]]])


(defn startpage-view
  " A view that represents the first page of meetly participation or creation. "
  []
  [startpage-content])