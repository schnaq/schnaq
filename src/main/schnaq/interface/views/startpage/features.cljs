(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.startpage.preview-statements :as examples]))

(defn- example-question
  "Box describing what schnaq does and why"
  []
  [:div.my-5.py-5
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

(defn- hybrid-questioning []
  [rows/row-builder-text-left
   [rows/build-text-box :startpage.information.meetings]
   [:img.shadow-lg.rounded-2
    {:src (img-path :startpage.information/meeting)}]])

(defn- feature-box
  ;; TODO früher auf einzelne Karten umbrechen
  "A Single feature box that can be put in a row. All inputs are keys."
  [title body icon-key]
  [:div.card.panel-white.text-center
   [:div.card-body
    [:div.display-6.text-typography.mb-3.card-title (labels title)]
    [icon icon-key "mb-3 mx-auto card-text" {:size "3x"}]
    [:p.text-justify.card-text (labels body)]]])

(defn- how-does-schnaq-work
  "Arguments for getting schnaq in three columns."
  []
  [:div.mt-lg-5
   [:h3.h2.text-center (labels :startpage.feature-box/heading)]
   [:div.card-deck.py-3
    [feature-box
     :startpage.feature-box.know-how/title
     :startpage.feature-box.know-how/body
     :chalkboard-teacher]
    ;; This block is used to break the card deck into one card per row for devices smaller than md
    ;; Without this only sm devices break.
    [:div.w-100.d-none.d-sm-block.d-lg-none.py-2]
    [feature-box
     :startpage.feature-box.discussion/title
     :startpage.feature-box.discussion/body
     :qrcode]
    [:div.w-100.d-none.d-sm-block.d-lg-none.py-2]
    [feature-box
     :startpage.feature-box.learnings/title
     :startpage.feature-box.learnings/body
     :user/group]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [how-does-schnaq-work]
   [example-question]
   [hybrid-questioning]
   [use-it-anywhere]
   [schnaq-promise]])
