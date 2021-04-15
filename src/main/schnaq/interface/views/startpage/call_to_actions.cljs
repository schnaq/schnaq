(ns schnaq.interface.views.startpage.call-to-actions
  (:require [schnaq.interface.text.display-data :refer [fa labels video img-path]]
            [re-frame.core :as rf]))

(defn- header-animation
  "Display header animation video."
  []
  [:section.panel-light-background.mb-3
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

(defn- social-proof
  "A small section showing the user, that the risk was already taken by others."
  []
  [:p.text-social-proof.text-center.pt-2
   [:img.social-proof-icon
    {:src (img-path :schnaqqifant/white)}]
   "schnaq hat schon in " [:b 350] " Diskussionen geholfen " [:b 2200] " mal Wissen auszutauschen."])

(defn features-call-to-action
  "Displays a list of features with a call-to-action button to start a schnaq"
  []
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.py-lg-4.pr-lg-5
    [header-animation]
    [start-schnaq-button]
    [social-proof]]
   [:div.col-lg-6.py-lg-5.pt-5
    [bullet-point :clipboard :feature/what]
    [bullet-point :user/group :feature/share]
    [bullet-point :site-map :feature/graph]
    [bullet-point :search :feature/processing]
    [bullet-point :shield :feature/secure]]])