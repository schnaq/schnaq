(ns meetly.meeting.interface.views.startpage
  (:require [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.text.display-data :as data]
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
  [:section.py-3
   [:div.container
    [:div.row
     [:div.col-lg-4.text-center
      [:img.img-fluid.shadow {:src (data/img-path :woman-pointing)}]]
     [:div.col-lg-8.text-center
      [:div.row.mt-4
       [:div.mx-auto.col-lg-10
        [:h3.mb-1 (data/labels :create-your-meeting)]
        [:br] [:br]
        [:button.btn.button-secondary
         {:type " button "
          :on-click #(rf/dispatch [:navigate :routes/meetings.create])}
         [:h4 (data/labels :create-meetly-button)]]
        [:p.pt-4 (data/labels :create-your-meeting-sub)]]]]]]])


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
   [header]
   [:div.container
    [call-to-action]
    [icons-grid]]])


(defn startpage-view
  " A view that represents the first page of meetly participation or creation. "
  []
  [startpage-content])