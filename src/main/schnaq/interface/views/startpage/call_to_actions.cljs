(ns schnaq.interface.views.startpage.call-to-actions
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]))

(defn- header-screenshot
  "Display header screenshot of an example discussion."
  []
  [:section.above-the-fold-screenshot
   [:img.taskbar-background.mb-2 {:src (img-path :how-to/taskbar)}]
   [motion/zoom-image {:class "img-fluid"
                       :src (img-path :startpage.screenshots/qanda)}]])

(defn- start-schnaq-button
  "Tell user to create a schnaq now."
  []
  [:section.mt-5.text-center
   [buttons/anchor-big
    [:<> (labels :schnaq.startpage.cta/button)
     [icon :arrow-right "ml-2"]]
    (rfe/href :routes.schnaq/create)
    "btn-secondary d-inline-block"]])

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
   [:div.col-lg-6.col-xxxl-7.py-lg-4.pr-lg-5.my-auto
    [:h1 (labels :startpage/subheading)]
    [:p.lead (labels :startpage/hook)]
    [start-schnaq-button]
    [social-proof]]
   [:div.col-lg-6.col-xxxl-4.pb-lg-4
    [header-screenshot]]])
