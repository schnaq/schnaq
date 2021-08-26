(ns schnaq.interface.views.startpage.features
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.rows :as rows]))

(defn- what-is-schnaq
  "Box describing schnaq and its advantages"
  []
  [rows/image-left
   :startpage.example/statements
   :startpage.information.know-how
   true "video-background-primary-with-shadow"])

(defn- schnaq-promise
  "Box describing schnaq and its advantages"
  []
  [:div.dot-background
   [rows/image-right
    :startpage.example/dashboard
    :startpage.information.positioning
    true "video-background-primary-with-shadow"]])

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
   [:div.row.pt-5
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
   [:p.text-center.pb-5
    [:a.btn.btn-primary.text-center
     {:href (rfe/href :routes.schnaq/create)}
     (labels :startpage.feature-box/explore-schnaq)]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [what-is-schnaq]
   [schnaq-promise]
   [feature-columns]])
