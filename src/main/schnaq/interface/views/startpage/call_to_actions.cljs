(ns schnaq.interface.views.startpage.call-to-actions
  (:require [schnaq.interface.text.display-data :refer [fa labels video]]
            [re-frame.core :as rf]))

(defn- header-animation
  "Display header animation video."
  []
  [:section
   [:video.w-100.startpage-animation {:auto-play true :loop false :muted true :plays-inline true}
    [:source {:src (video :start-page.features.sample-discussion/webm) :type "video/webm"}]
    [:source {:src (video :start-page.features.sample-discussion/mp4) :type "video/mp4"}]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section
   [:button.button-call-to-action.w-100
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
    (labels :schnaq.startpage.cta/button)]])

(defn- bullet-point
  "Display a bullet-point with a leading icon.
  _call-to-action.scss contains the related styling."
  [icon-label desc-label]
  [:div.row.py-3
   [:div.col-2.col-lg-1
    [:i.icon-points-icon {:class (str "fas fa-2x " (fa icon-label))}]]
   [:div.col-10.col-lg-11
    [:span.icon-points-text (labels desc-label)]]])

(defn features-call-to-action
  "Displays a list of features with a call-to-action button to start a schnaq"
  []
  [:section.row {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.py-lg-4.pr-lg-5
    [header-animation]
    [start-schnaq-button]]
   [:div.col-lg-6.py-lg-5
    [bullet-point :clipboard :feature/what]
    [bullet-point :user/group :feature/share]
    [bullet-point :site-map :feature/graph]
    [bullet-point :search :feature/processing]
    [bullet-point :shield :feature/secure]]])