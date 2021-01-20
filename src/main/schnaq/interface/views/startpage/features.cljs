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
   :start-page.features.sample-discussion/webm
   :start-page.features.sample-discussion/mp4
   :startpage.features.discussion
   false])

(defn- graph-visualization
  "Feature box showcasing the graph."
  []
  [rows/image-right
   :startpage.features/discussion-graph
   :startpage.features.graph])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [meeting-organisation]
   [structured-discussions]
   [graph-visualization]])
