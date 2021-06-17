(ns schnaq.interface.views.startpage.features
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.rows :as rows]))

(defn- what-is-schnaq
  "Box describing schnaq and its advantages"
  []
  [rows/video-right
   :animation-discussion/webm
   :animation-discussion/mp4
   :startpage.objections
   true "video-background-primary"])

(defn- schnaq-promise
  "Box describing schnaq and its advantages"
  []
  [rows/video-left
   :start-page.work-together/webm
   :start-page.work-together/mp4
   :startpage.promise
   true "video-background-primary"])

(defn- elephant-in-the-room
  "Feature box showcasing the elephant in the room."
  []
  [rows/image-right
   :schnaqqifant/admin
   :startpage.elephant-in-the-room
   [:p.text-center.mb-0
    [:a.btn.btn-primary
     {:href (rfe/href :routes/about-us)}
     (labels :startpage.elephant-in-the-room/button)]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [what-is-schnaq]
   [schnaq-promise]
   [elephant-in-the-room]])
