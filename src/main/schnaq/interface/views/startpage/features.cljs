(ns schnaq.interface.views.startpage.features
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels fa]]
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
  [rows/video-right
   :start-page.address-elephant/webm
   :start-page.address-elephant/mp4
   :startpage.elephant-in-the-room
   true "video-background-primary"
   [:p.text-center.mb-0
    [:a.btn.btn-primary
     {:href (rfe/href :routes/about-us)}
     (labels :startpage.elephant-in-the-room/button)]]])

(defn- feature-box
  "A Single feature box that can be put in a row. All inputs are keys."
  [title icon body]
  [:div.col-12.col-md-4
   [:h4.text-center (labels title)]
   [:p.text-center.text-primary.mt-0.py-0
    [:i {:class (str " m-auto fas fa-3x " (fa icon))}]]
   [:p.text-justify (labels body)]])

(defn- feature-columns
  "Arguments for getting schnaq in three columns."
  []
  [:<>
   [:div.row
    [feature-box
     :startpage.feature-box.know-how/title
     :book
     :startpage.feature-box.know-how/body]
    [feature-box
     :startpage.feature-box.discussion/title
     :comment
     :startpage.feature-box.discussion/body]
    [feature-box
     :startpage.feature-box.learnings/title
     :lightbulb
     :startpage.feature-box.learnings/body]]
   [:p.text-center
    ;; TODO let it go to the feature site when its ready
    [:a.btn.btn-primary.text-center "Entdecke schnaq"]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [what-is-schnaq]
   [schnaq-promise]
   [elephant-in-the-room]
   [feature-columns]])
