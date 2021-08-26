(ns schnaq.interface.views.startpage.call-to-actions
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels img-path video]]))

(defn- _header-animation
  "Display header animation video."
  []
  [:section.animation-background.mb-3
   [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
   [:video.w-100.video-scalable-with-shadow-and-border {:auto-play true :loop true :muted true :plays-inline true}
    [:source {:src (video :how-to.discussion/webm) :type "video/webm"}]
    [:source {:src (video :how-to.discussion/mp4) :type "video/mp4"}]]])

(defn- header-screenshot
  "Display header screenshot of an example discussion."
  []
  [:section.above-the-fold-screenshot
   [:img.taskbar-background.mb-2 {:src (img-path :how-to/taskbar)}]
   [:img.img-fluid {:src (img-path :startpage.example/discussion)}]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.mt-5
   [:div.d-flex.flex-row
    [:div.display-6.text-white.my-auto.mr-3 (labels :schnaq.startpage.cta/button)]
    [:button.button-call-to-action
     {:type "button"
      :on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
     [:i.m-auto {:class (str "fa " (fa :arrow-right))}]]]])

(defn- social-proof
  "A small section showing the user, that the risk was already taken by others."
  []
  [:p.text-social-proof.text-center.pt-2
   [:img.social-proof-icon
    {:src (img-path :schnaqqifant/white)}]
   (labels :startpage.social-proof/numbers)])

(defn- bullet-point
  "Display a bullet-point with a leading icon.
  _call-to-action.scss contains the related styling."
  [icon-label desc-label]
  [:div.row.py-2
   [:div.col-2.col-lg-1.my-auto
    [:i.icon-points-icon {:class (str "fas fa-2x " (fa icon-label))}]]
   [:div.col-10.col-lg-11
    [:span.icon-points-text (labels desc-label)]]])

(defn- _intro-bullets
  "The teaser bullets for above the fold."
  []
  [:<>
   [bullet-point :clipboard :feature/organization]
   [bullet-point :comment :feature/inputs]
   [bullet-point :puzzle :feature/integration]
   [bullet-point :user/group :feature/equality]
   [bullet-point :shield :feature/datenschutz]])

(defn features-call-to-action
  "Displays a list of features with a call-to-action button to start a schnaq"
  []
  [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
   [:div.col-lg-5.py-lg-4.pr-lg-5
    [:h1 (labels :startpage/subheading)]
    [start-schnaq-button]
    [social-proof]]
   [:div.col-lg-7.py-lg-4
    [header-screenshot]]])