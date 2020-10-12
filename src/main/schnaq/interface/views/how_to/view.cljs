(ns schnaq.interface.views.how-to.view
  (:require [schnaq.interface.views.base :as base]
            [schnaq.interface.text.display-data :refer [labels img-path video]]
            [re-frame.core :as rf]))

(defn- text-box
  "Text box with title and a body."
  [title body]
  [:article.feature-text-box.pb-5
   [:h1 (labels title)]
   [:p (labels body)]])

(defn- feature-row-video-left
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true :plays-inline true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]
   [:div.col-12.col-lg-5.offset-lg-1
    [text-box title body]]])

(defn- feature-row-video-right
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [text-box title body]]
   [:div.col-12.col-lg-6.offset-lg-1
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true :plays-inline true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]])

(defn- call-to-action-row
  "Call to action row with button."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [text-box
     :how-to.call-to-action/title
     :how-to.call-to-action/body]]
   [:div.col-12.col-lg-6.text-center
    [:button.button-secondary.font-200
     {:type "button"
      :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/create])}
     (labels :startpage.button/create-schnaq)]]])

(defn- admin []
  [feature-row-video-left
   :how-to.admin/webm
   :how-to.admin/mp4
   :how-to.admin/title
   :how-to.admin/body])

(defn- agenda []
  [feature-row-video-right
   :how-to.agenda/webm
   :how-to.agenda/mp4
   :how-to.agenda/title
   :how-to.agenda/body])

(defn- create []
  [feature-row-video-left
   :how-to.create/webm
   :how-to.create/mp4
   :how-to.create/title
   :how-to.create/body])

(defn- why []
  [feature-row-video-right
   :how-to.why/webm
   :how-to.why/mp4
   :how-to.why/title
   :how-to.why/body])

(defn- content []
  [:<>
   [base/nav-header]
   [base/header
    (labels :how-to.title)]
   [:div.container.chat-background.py-5
    ;; how to videos
    [:div.pb-5.bubble-background [why]]
    [:div.how-to-video-row [create]]
    [:div.how-to-video-row [agenda]]
    [:div.how-to-video-row [admin]]
    ;; start schnaq
    [call-to-action-row]]])

(defn view []
  [content])