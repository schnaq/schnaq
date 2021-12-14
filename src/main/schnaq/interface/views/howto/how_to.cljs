(ns schnaq.interface.views.howto.how-to
  (:require [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.howto.elements :as elements]
            [schnaq.interface.views.pages :as pages]))

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

(defn- content []
  [pages/with-nav-and-header
   {:page/heading (labels :how-to/title)
    :page/vertical-header? true}
   [:div.container.chat-background.py-5
    ;; how to videos
    [:div.how-to-video-row [schnaq]]
    [:div.how-to-video-row [pro-con]]]])

(defn view []
  [content])

(defn- back-to-schnaq-row
  "Call to action row with button."
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6.text-center
    [:button.button-secondary.font-200
     {:on-click #(rf/dispatch [:discussion.history/time-travel 1])}
     (labels :how-to/back-to-start)]]
   [:div.col-12.col-lg-6
    [elements/text-box
     :how-to.call-to-action/title
     :how-to.call-to-action/body]]])

(defn- embedded-content []
  [pages/with-discussion-header
   {:page/heading (labels :how-to/title)
    :page/vertical-header? true}
   [:div.container.chat-background.py-5
    ;; how to videos
    [:div.how-to-video-row [schnaq]]
    [:div.how-to-video-row [pro-con]]
    ;; start schnaq
    [back-to-schnaq-row]]])

(defn embedded-view []
  [embedded-content])
