(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.views.common :as common]))

(defn- build-feature-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  [text-namespace]
  (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
    [:article.feature-text-box.pb-5
     [:p.lead.mb-1 (labels (prepend-namespace :lead))]
     [:h5 (labels (prepend-namespace :title))]
     [:p (labels (prepend-namespace :body))]
     [:div.btn.btn-outline-dark
      (labels :startpage.features/more-information)]]))

(defn- feature-row-image-left
  "Build a feature row, where the image is located on the left side."
  [image-key text-namespace]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [:img.img-fluid {:src (img-path image-key)}]]
   [:div.col-12.col-lg-6.offset-lg-1
    [build-feature-text-box text-namespace]]])

(defn- feature-row-image-right
  "Build a feature row, where the image is located on the right side."
  [image-key text-namespace]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [build-feature-text-box text-namespace]]
   [:div.col-12.col-lg-5.offset-lg-1
    [:img.img-fluid {:src (img-path image-key)}]]])

(defn- feature-meeting-organisation
  "Featuring meeting-organisation with an image."
  []
  (feature-row-image-right
    :startpage.features/meeting-organisation
    :startpage.features.meeting-organisation))

(defn- feature-structured-discussions
  "Overview of structured discussions."
  []
  (feature-row-image-left
    :startpage.features/sample-discussion
    :startpage.features.discussion))


;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [feature-meeting-organisation]
   [feature-structured-discussions]])