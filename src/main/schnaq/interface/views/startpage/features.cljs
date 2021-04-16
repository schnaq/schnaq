(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.utils.rows :as rows]))

(defn- meeting-organisation
  "Featuring meeting-organisation with an image."
  []
  [rows/video-right
   :start-page.work-together/webm
   :start-page.work-together/mp4
   :startpage.features.meeting-organisation])

(defn- structured-discussions
  "Overview of structured discussions."
  []
  [rows/video-left
   :animation-discussion/webm
   :animation-discussion/mp4
   :startpage.features.discussion
   true "video-background-primary"])

(defn- graph-visualization
  "Feature box showcasing the graph."
  []
  [rows/image-right
   :schnaqqifant/admin
   :startpage.features.graph])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [meeting-organisation]
   [structured-discussions]
   [graph-visualization]])
