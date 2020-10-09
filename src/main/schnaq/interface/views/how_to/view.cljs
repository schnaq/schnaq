(ns schnaq.interface.views.how-to.view
  (:require [schnaq.interface.views.base :as base]
            [schnaq.interface.text.display-data :refer [labels img-path video]]))

(defn build-feature-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  [title body]
  [:article.feature-text-box.pb-5
   [:h1 (labels title)]
   [:p (labels body)]])

(defn- feature-row-image-left
  "Build a feature row, where the image is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]
   [:div.col-12.col-lg-5.offset-lg-1
    [build-feature-text-box title body]]])

(defn- feature-row-image-right
  "Build a feature row, where the image is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [build-feature-text-box title body]]
   [:div.col-12.col-lg-6.offset-lg-1
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]])

(defn- admin []
  [feature-row-image-left
   :how-to.admin/webm
   :how-to.admin/mp4
   :how-to.admin/title
   :how-to.admin/body])

(defn- agenda []
  [feature-row-image-right
   :how-to.agenda/webm
   :how-to.agenda/mp4
   :how-to.agenda/title
   :how-to.agenda/body])

(defn- create []
  [feature-row-image-left
   :how-to.create/webm
   :how-to.create/mp4
   :how-to.create/title
   :how-to.create/body])

(defn- why []
  [feature-row-image-right
   :how-to.why/webm
   :how-to.why/mp4
   :how-to.why/title
   :how-to.why/body])

(defn- content []
  [:<>
   [base/nav-header]
   [base/header
    (labels :how-to.title)]
   [:div.how-to-container.chat-background.py-5
    [:div.pb-5.bubble-background [why]]
    [:div.how-to-video-row [create]]
    [:div.how-to-video-row [agenda]]
    [:div.how-to-video-row [admin]]]])

(defn view []
  [content])