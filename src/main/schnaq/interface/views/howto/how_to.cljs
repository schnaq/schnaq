(ns schnaq.interface.views.howto.how-to
  (:require [schnaq.interface.text.display-data :refer [labels]]
            [re-frame.core :as rf]
            [schnaq.interface.views.howto.elements :as elements]
            [schnaq.interface.views.pages :as pages]))

(defn- call-to-action-row
  "Call to action row with button."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6.text-center
    [:button.button-secondary.font-200
     {:type "button"
      :on-click #(rf/dispatch [:navigation/navigate :routes.schnaq/create])}
     (labels :schnaq.create.button/save)]]
   [:div.col-12.col-lg-6
    [elements/text-box
     :how-to.call-to-action/title
     :how-to.call-to-action/body]]])

(defn- pro-con []
  [elements/feature-row-video-right
   :how-to.pro-con/webm
   :how-to.pro-con/mp4
   :how-to.pro-con/title
   :how-to.pro-con/body])

(defn- schnaq []
  [elements/feature-row-video-left
   :how-to.discussion/webm
   :how-to.discussion/mp4
   :how-to.schnaq/title
   :how-to.schnaq/body])


(defn- admin []
  [elements/feature-row-video-right
   :how-to.admin/webm
   :how-to.admin/mp4
   :how-to.admin/title
   :how-to.admin/body])

(defn- create []
  [elements/feature-row-video-left
   :how-to.create/webm
   :how-to.create/mp4
   :how-to.create/title
   :how-to.create/body])

(defn- why []
  [elements/feature-row-video-right
   :how-to.why/webm
   :how-to.why/mp4
   :how-to.why/title
   :how-to.why/body])

(defn- content []
  [pages/with-nav-and-header
   {:page/heading (labels :how-to/title)}
   [:div.container.chat-background.py-5
    ;; how to videos
    [:div.pb-5.bubble-background [why]]
    [:div.how-to-video-row [create]]
    [:div.how-to-video-row [admin]]
    [:div.how-to-video-row [schnaq]]
    [:div.how-to-video-row [pro-con]]
    ;; start schnaq
    [call-to-action-row]]])

(defn view []
  [content])