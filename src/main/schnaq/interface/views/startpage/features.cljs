(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.rows :as rows]))

(defn- what-is-schnaq
  "Box describing what schnaq does and why"
  []
  [rows/image-left
   :startpage.example/statements
   :startpage.information.know-how
   true "video-background-primary-with-shadow"])

(defn- schnaq-promise
  "Box describing schnaq's promise to the user"
  []
  [:div.dot-background
   [rows/image-right
    :startpage.example/dashboard
    :startpage.information.positioning
    true "video-background-primary-with-shadow"]])

(defn- feature-box
  "A Single feature box that can be put in a row. All inputs are keys."
  [title body]
  [:div.col-12.col-md-4
   [:div.panel-white.mx-1.shadow.py-5.text-center
    [:div.display-6.text-purple.mb-5 (labels title)]
    [:p.text-justify (labels body)]]])

(defn- feature-columns
  "Arguments for getting schnaq in three columns."
  []
  [:<>
   [:div.row.py-5
    [feature-box
     :startpage.feature-box.know-how/title
     :startpage.feature-box.know-how/body]
    [feature-box
     :startpage.feature-box.discussion/title
     :startpage.feature-box.discussion/body]
    [feature-box
     :startpage.feature-box.learnings/title
     :startpage.feature-box.learnings/body]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [what-is-schnaq]
   [schnaq-promise]
   [feature-columns]])
