(ns schnaq.interface.views.startpage.features
  (:require [schnaq.interface.text.display-data :refer [labels fa img-path]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]))

(defn- build-feature-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  [text-namespace]
  (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
    [:article.feature-text-box.pb-5
     [:p.lead.mb-1 (labels (prepend-namespace :lead))]
     [:h5 (labels (prepend-namespace :title))]
     [:p (labels (prepend-namespace :body))]]))

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

(defn- meeting-organisation
  "Featuring meeting-organisation with an image."
  []
  [feature-row-image-right
   :startpage.features/meeting-organisation
   :startpage.features.meeting-organisation])

(defn- structured-discussions
  "Overview of structured discussions."
  []
  [feature-row-image-left
   :startpage.features/sample-discussion
   :startpage.features.discussion])

(defn- graph-visualization
  "Feature box showcasing the graph."
  []
  [feature-row-image-right
   :startpage.features/discussion-graph
   :startpage.features.graph])

(defn- features-meeting
  "A site demonstrating the features of schnaqs meeting capabilities."
  []
  (pages/with-nav-and-header
    {:page/heading (labels :feature.meetings/lead)}
    [:div.container.py-4
     [feature-row-image-right
      :feature.meetings/hero-image
      :feature.meetings]
     [:h3.text-center.pb-4 (labels :feature.meetings/features-subheading)]
     [feature-row-image-left
      :feature.meetings/schedule-meetings
      :feature.meetings.schedule]
     [feature-row-image-right
      :startpage.features/sample-discussion
      :feature.meetings.discuss]
     [feature-row-image-left
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
     [feature-row-image-right
      :feature.discussions/hero-image
      :feature.discussions]
     [:h3.text-center.pb-4 (labels :feature.discussions/features-subheading)]
     [feature-row-image-left
      :feature.discussions/create-discussion-spaces
      :feature.discussions.spaces]
     [feature-row-image-right
      :startpage.features/sample-discussion
      :feature.discussions.discuss]
     [feature-row-image-left
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
     [feature-row-image-right
      :feature.knowledge/hero-image
      :feature.knowledge.general]
     [:h3.text-center.pb-4 (labels :feature.knowledge/features-subheading)]
     [feature-row-image-left
      :startpage.features/sample-discussion
      :feature.knowledge.discussions]
     [feature-row-image-right
      :feature.knowledge/overview
      :feature.knowledge.database]
     [feature-row-image-left
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

(defn- data-privacy
  "A section which describes, what we do with data care."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [:p.display-1.text-center.text-primary [:i {:class (str "m-auto fas fa-lg " (fa :shield))}]]]
   [:div.col-12.col-lg-6.offset-lg-1
    [:article.feature-text-box.pb-5
     [:p.lead.mb-1 (labels :features.privacy/lead)]
     [:h5 (labels :features.privacy/title)]
     [:p (labels :features.privacy/body)]]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [meeting-organisation]
   [structured-discussions]
   [graph-visualization]
   [data-privacy]])

(defn meeting-features-view []
  [features-meeting])

(defn discussion-features-view []
  [features-discussion])

(defn knowledge-features-view []
  [features-knowledge])