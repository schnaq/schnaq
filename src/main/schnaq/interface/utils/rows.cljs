(ns schnaq.interface.utils.rows
  (:require [schnaq.interface.text.display-data :refer [labels img-path video]]
            [schnaq.interface.views.common :as common]))

(defn- build-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  ([text-namespace]
   [build-text-box text-namespace nil])
  ([text-namespace more]
   (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
     [:article.feature-text-box
      [:h3.h1.text-purple.mb-5 (labels (prepend-namespace :title))]
      [:section (labels (prepend-namespace :body))
       (when more
         more)]])))

(defn- row-builder-text-right
  "Generic builder to align text and asset. Here, text is on the right
  and the remainder is on the left."
  [left right]
  [:div.row.feature-row
   [:div.col-12.col-lg-5.my-auto left]
   [:div.col-12.col-lg-6.offset-lg-1.my-auto right]])

(defn- row-builder-text-left
  "Build a row, like the feature rows. Here, the text is on the left side."
  [left right]
  [:div.row.feature-row
   [:div.col-12.col-lg-6.my-auto left]
   [:div.col-12.col-lg-5.offset-lg-1.my-auto right]])


;; -----------------------------------------------------------------------------

(defn image-left
  "Build a row, where the image is located on the left side."
  [image-key text-namespace]
  [row-builder-text-right
   [:img.img-fluid {:src (img-path image-key)}]
   [build-text-box text-namespace]])

(defn image-right
  "Build a row, where the image is located on the right side."
  [image-key text-namespace]
  [row-builder-text-left
   [build-text-box text-namespace]
   [:img.img-fluid {:src (img-path image-key)}]])

(defn video-left
  "Feature row where the video is located on the right side."
  [video-key-webm video-key-mp4 text-namespace & [looping? video-class]]
  (let [attributes {:auto-play true :muted true :plays-inline true}]
    [row-builder-text-right
     [:video.w-100.feature-animations
      (cond-> attributes
              looping? (assoc :loop looping?)
              video-class (assoc :class video-class))
      [:source {:src (video video-key-webm) :type "video/webm"}]
      [:source {:src (video video-key-mp4) :type "video/mp4"}]]
     [build-text-box text-namespace]]))

(defn video-right
  "Feature row where the video is located on the right side."
  [video-key-webm video-key-mp4 text-namespace & [looping? video-class more]]
  (let [attributes {:auto-play true :muted true :plays-inline true}]
    [row-builder-text-left
     [build-text-box text-namespace more]
     [:video.w-100.feature-animations
      (cond-> attributes
              looping? (assoc :loop looping?)
              video-class (assoc :class video-class))
      [:source {:src (video video-key-webm) :type "video/webm"}]
      [:source {:src (video video-key-mp4) :type "video/mp4"}]]]))

(defn icon-right
  "Build a row with text on the left side and the icon on the right side."
  [icon text-namespace]
  [row-builder-text-left
   [build-text-box text-namespace]
   [:div.display-1.text-center.text-primary icon]])

(defn icon-left
  "Build a row with text on the right side and the icon on the left side."
  [icon text-namespace]
  [row-builder-text-right
   [:div.display-1.text-center.text-primary icon]
   [build-text-box text-namespace]])
