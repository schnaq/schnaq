(ns schnaq.interface.views.startpage.call-to-actions
  (:require [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [fa img-path labels]]
            [re-frame.core :as rf]))

(defn- header-animation
  "Display header animation video."
  []
  [:section
   [:video.w-100.video-background-primary.startpage-animation {:auto-play true :loop true :muted true :plays-inline true}
    [:source {:src (img-path :animation-discussion) :type "video/webm"}]
    [:source {:src (img-path :animation-discussion-mp4) :type "video/mp4"}]]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section
   [:button.button-call-to-action.w-100
    {:type "button"
     :on-click #(rf/dispatch [:navigation/navigate :routes.brainstorm/create])}
    (labels :brainstorm.buttons/start-now)]])

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
  [:div.row {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-6.py-lg-4.pr-lg-5
    [header-animation]
    [start-schnaq-button]]
   [:div.col-lg-6.py-lg-5
    [bullet-point :clipboard :feature/what]
    [bullet-point :share :feature/share]
    [bullet-point :user/group :feature/participate]
    [bullet-point :user/shield :feature/private-public]
    [bullet-point :shield :feature/secure]]])

(defn- spotlight-element [title image link]
  [:div.spotlight-discussion.clickable-no-hover
   [:a.no-text-decoration {:href link}
    [:img.spotlight-discussion-image {:src image}]
    [:div.spotlight-discussion-title
     [:h6 title]]]])

(defn spotlight-discussions
  "Display a row of clickable spotline discussions. Links are currently hardcoded"
  []
  [:div.my-4.my-lg-5
   [:h4 (labels :startpage.call-to-action/discuss-spotlight-topics)]
   [:div.row
    ; credit https://pixabay.com/illustrations/brain-leaves-sustainability-organ-5591471/
    [:div.col-lg-4
     [spotlight-element "Mein Beitrag für eine nachhaltigere Welt"
      (img-path :spotlight.eco-brain)
      (config/spotlight-1)]]
    ; credit https://pixabay.com/illustrations/covid-19-work-from-home-quarantine-4938932/
    [:div.col-lg-4
     [spotlight-element "Remote-Arbeiten in Zeiten von Corona"
      (img-path :spotlight.home-office)
      (config/spotlight-2)]]
    ; credit https://www.freeimg.net/photo/1188165/merkel-chancellor-angelamerkel-cdu
    [:div.col-lg-4
     [spotlight-element "Nächste Bundesklanzer:in"
      (img-path :spotlight.merkel)
      (config/spotlight-3)]]]])