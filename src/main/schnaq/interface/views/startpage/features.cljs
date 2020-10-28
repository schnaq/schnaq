(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]))

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
  [rows/image-left
   :startpage.features/sample-discussion
   :startpage.features.discussion])

(defn- graph-visualization
  "Feature box showcasing the graph."
  []
  [rows/image-right
   :startpage.features/discussion-graph
   :startpage.features.graph])

(defn- features-meeting
  "A site demonstrating the features of schnaqs meeting capabilities."
  []
  (pages/with-nav-and-header
    {:page/heading (labels :feature.meetings/lead)}
    [:div.container.py-4
     [rows/image-right
      :feature.meetings/hero-image
      :feature.meetings]
     [:h3.text-center.pb-4 (labels :feature.meetings/features-subheading)]
     [rows/image-left
      :feature.meetings/schedule-meetings
      :feature.meetings.schedule]
     [rows/image-right
      :startpage.features/sample-discussion
      :feature.meetings.discuss]
     [rows/image-left
      :startpage.features/admin-center
      :feature.meetings.admin-center]
     [:section.feature-text-box
      [:h3.text-center.pb-4 (labels :feature.meetings/tbd-subheading)]
      [:p (labels :feature.meetings/tbd-lead)]
      [:ul
       [:li (labels :feature.meetings.tbd/teams)]
       [:li (labels :feature.meetings.tbd/outlook)]
       [:li (labels :feature.meetings.tbd/protocols)]
       [:li (labels :feature.meetings.tbd/prereads)]
       [:li (labels :feature.meetings.tbd/assignments)]
       [:li (labels :feature.meetings.tbd/timeboxing)]
       [:li (labels :feature.meetings.tbd/task-tracking)]]
      [:p (labels :feature.meetings/feedback)]]]))

(defn- features-discussion
  "A site demonstrating the features of schnaqs discussion capabilities."
  []
  (pages/with-nav-and-header
    {:page/heading (labels :feature.discussions/lead)}
    [:div.container.py-4
     [rows/image-right
      :feature.discussions/hero-image
      :feature.discussions]
     [:h3.text-center.pb-4 (labels :feature.discussions/features-subheading)]
     [rows/image-left
      :feature.discussions/create-discussion-spaces
      :feature.discussions.spaces]
     [rows/image-right
      :startpage.features/sample-discussion
      :feature.discussions.discuss]
     [rows/image-left
      :startpage.features/discussion-graph
      :feature.discussions.graph]
     [:section.feature-text-box
      [:h3.text-center.pb-4 (labels :feature.meetings/tbd-subheading)]
      [:p (labels :feature.meetings/tbd-lead)]
      [:ul
       [:li (labels :feature.discussions.tbd/reports)]
       [:li (labels :feature.discussions.tbd/wikis)]
       [:li (labels :feature.discussions.tbd/ideas)]
       [:li (labels :feature.discussions.tbd/navigation)]
       [:li (labels :feature.discussions.tbd/connect)]
       [:li (labels :feature.discussions.tbd/bot)]]
      [:p (labels :feature.meetings/feedback)]]]))

(defn- features-knowledge
  "Presenting the idea of knowledge aggregation."
  []
  (pages/with-nav-and-header
    {:page/heading (labels :feature.knowledge/lead)
     :page/subheading (labels :feature.knowledge/subheading)}
    [:div.container.py-4
     [rows/image-right
      :feature.knowledge/hero-image
      :feature.knowledge.general]
     [:h3.text-center.pb-4 (labels :feature.knowledge/features-subheading)]
     [rows/image-left
      :startpage.features/sample-discussion
      :feature.knowledge.discussions]
     [rows/image-right
      :feature.knowledge/overview
      :feature.knowledge.database]
     [rows/image-left
      :startpage.features/discussion-graph
      :feature.knowledge.change-of-facts]
     [:section.feature-text-box
      [:h3.text-center.pb-4 (labels :feature.meetings/tbd-subheading)]
      [:p (labels :feature.meetings/tbd-lead)]
      [:ul
       [:li (labels :feature.knowledge.tbd/wiki)]
       [:li (labels :feature.knowledge.tbd/search)]
       [:li (labels :feature.knowledge.tbd/evaluation)]
       [:li (labels :feature.knowledge.tbd/live-changes)]
       [:li (labels :feature.knowledge.tbd/changes-over-time)]
       [:li (labels :feature.knowledge.tbd/accounts)]]
      [:p (labels :feature.meetings/feedback)]]]))


;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [meeting-organisation]
   [structured-discussions]
   [graph-visualization]])

(defn meeting-features-view []
  [features-meeting])

(defn discussion-features-view []
  [features-discussion])

(defn knowledge-features-view []
  [features-knowledge])