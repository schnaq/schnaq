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

(defn- use-it-anywhere []
  [:div.my-5.py-lg-5
   [rows/row-builder-text-right
    [:img.shadow-lg.rounded-2
     {:src (img-path :startpage.information/anywhere)}]
    [rows/build-text-box :startpage.information.anywhere]]])

(defn- less-meetings []
  [rows/row-builder-text-left
   [rows/build-text-box :startpage.information.meetings]
   [:img.shadow-lg.rounded-2
    {:src (img-path :startpage.information/meeting)}]])

(defn- feature-box
  "A Single feature box that can be put in a row. All inputs are keys."
  [title body]
  [:div.col-12.col-md-4
   [:div.panel-white.mx-1.shadow.py-4.text-center
    [:div.display-6.text-purple.mb-3 (labels title)]
    [:p.text-justify (labels body)]]])

(defn- how-does-schnaq-work
  "Arguments for getting schnaq in three columns."
  []
  [:div.mt-lg-5
   [:h3.h2.text-center (labels :startpage.feature-box/heading)]
   [:div.row.py-3
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
   [how-does-schnaq-work]
   [:hr.mt-5.pb-5]
   [what-is-schnaq]
   [schnaq-promise]
   [use-it-anywhere]
   [less-meetings]])
