(ns schnaq.interface.views.startpage.call-to-actions
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [fa labels img-path]]))

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