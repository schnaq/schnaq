(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.startpage.preview-statements :as examples]))

(defn- what-is-schnaq
  "Box describing what schnaq does and why"
  []
  [:div.my-5
   [rows/row-builder-text-right-mobile-above
    [examples/display-example-statements]
    [rows/build-text-box :startpage.information.know-how]]])

(defn- schnaq-promise
  "Box describing schnaq's promise to the user"
  []
  [:div.my-5.py-lg-5
   [rows/row-builder-text-left
    [rows/build-text-box :startpage.information.positioning]
    [:div.example-dashboard-image
     [motion/zoom-image
      {:class "img-fluid shadow-lg rounded-2"
       :src (img-path :startpage.example/dashboard)}]]]])

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
  [:div.mt-lg-5
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
